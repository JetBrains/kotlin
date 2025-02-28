/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.search.ProjectScope
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.getLogger
import org.jetbrains.kotlin.cli.jvm.compiler.PsiBasedProjectFileSearchScope
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.unregisterFinders
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.jvmModularRoots
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.LanguageFeature.MultiPlatformProjects
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.checkers.registerExperimentalCheckers
import org.jetbrains.kotlin.fir.checkers.registerExtraCommonCheckers
import org.jetbrains.kotlin.fir.deserialization.ModuleDataProvider
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirBuiltinSyntheticFunctionInterfaceProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.syntheticFunctionInterfacesSymbolProvider
import org.jetbrains.kotlin.fir.session.*
import org.jetbrains.kotlin.library.metadata.resolver.impl.KotlinResolvedLibraryImpl
import org.jetbrains.kotlin.library.resolveSingleFileKlib
import org.jetbrains.kotlin.load.kotlin.PackageAndMetadataPartProvider
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.platform.isJs
import org.jetbrains.kotlin.platform.isWasm
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.platform.konan.isNative
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.singleValue
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDiagnosticCollectorService
import org.jetbrains.kotlin.test.frontend.fir.handlers.firDiagnosticCollectorService
import org.jetbrains.kotlin.test.model.FrontendFacade
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.NativeEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.WasmEnvironmentConfigurator
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.wasm.config.WasmConfigurationKeys
import java.nio.file.Paths
import org.jetbrains.kotlin.konan.file.File as KFile

open class FirFrontendFacade(testServices: TestServices) : FrontendFacade<FirOutputArtifact>(testServices, FrontendKinds.FIR) {
    override val additionalServices: List<ServiceRegistrationData>
        get() = listOf(
            service(::FirModuleInfoProvider),
            service(::FirDiagnosticCollectorService),
        )

    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(FirDiagnosticsDirectives)

    override fun shouldTransform(module: TestModule): Boolean {
        return shouldRunFirFrontendFacade(module, testServices)
    }

    private fun registerExtraComponents(session: FirSession) {
        testServices.firSessionComponentRegistrar?.registerAdditionalComponent(session)
    }

    override fun analyze(module: TestModule): FirOutputArtifact {
        val isMppSupported = module.languageVersionSettings.supportsFeature(MultiPlatformProjects)

        val sortedModules = if (isMppSupported) sortDependsOnTopologically(module) else listOf(module)

        val (moduleDataMap, moduleDataProvider) = initializeModuleData(sortedModules)

        val project = testServices.compilerConfigurationProvider.getProject(module)
        val extensionRegistrars = FirExtensionRegistrar.getInstances(project)
        val targetPlatform = module.targetPlatform(testServices)
        val predefinedJavaComponents = runIf(targetPlatform.isJvm()) {
            FirSharableJavaComponents(firCachesFactoryForCliMode)
        }
        val (projectEnvironment, librarySession) = createLibrarySession(
            module,
            project,
            Name.special("<${module.name}>"),
            testServices.firModuleInfoProvider.firSessionProvider,
            moduleDataProvider,
            testServices.compilerConfigurationProvider.getCompilerConfiguration(module),
            extensionRegistrars,
            predefinedJavaComponents
        )

        val firOutputPartForDependsOnModules = sortedModules.map {
            analyze(it, moduleDataMap[it]!!, targetPlatform, projectEnvironment, librarySession, extensionRegistrars, predefinedJavaComponents)
        }

        return FirOutputArtifactImpl(firOutputPartForDependsOnModules)
    }

    protected fun sortDependsOnTopologically(module: TestModule): List<TestModule> {
        return module.transitiveDependsOnDependencies(includeSelf = true, reverseOrder = true)
    }

    private fun initializeModuleData(modules: List<TestModule>): Pair<Map<TestModule, FirModuleData>, ModuleDataProvider> {
        val mainModule = modules.last()

        val targetPlatform = mainModule.targetPlatform(testServices)

        // the special name is required for `KlibMetadataModuleDescriptorFactoryImpl.createDescriptorOptionalBuiltIns`
        // it doesn't seem convincingly legitimate, probably should be refactored
        val moduleName = Name.special("<${mainModule.name}>")
        val binaryModuleData = BinaryModuleData.initialize(moduleName)

        val compilerConfigurationProvider = testServices.compilerConfigurationProvider
        val configuration = compilerConfigurationProvider.getCompilerConfiguration(mainModule)

        val libraryList = initializeLibraryList(mainModule, binaryModuleData, targetPlatform, configuration, testServices)

        val moduleInfoProvider = testServices.firModuleInfoProvider
        val moduleDataMap = mutableMapOf<TestModule, FirModuleData>()

        for (module in modules) {
            val regularModules = libraryList.regularDependencies + moduleInfoProvider.getRegularDependentSourceModules(module)
            val friendModules = libraryList.friendsDependencies + moduleInfoProvider.getDependentFriendSourceModules(module)
            val dependsOnModules = libraryList.dependsOnDependencies + moduleInfoProvider.getDependentDependsOnSourceModules(module)

            val moduleData = FirSourceModuleData(
                Name.special("<${module.name}>"),
                regularModules,
                dependsOnModules,
                friendModules,
                targetPlatform,
                isCommon = module.languageVersionSettings.supportsFeature(MultiPlatformProjects) && !module.isLeafModuleInMppGraph(testServices),
            )

            moduleInfoProvider.registerModuleData(module, moduleData)

            moduleDataMap[module] = moduleData
        }

        return moduleDataMap to libraryList.moduleDataProvider
    }

    private fun createLibrarySession(
        module: TestModule,
        project: Project,
        moduleName: Name,
        sessionProvider: FirProjectSessionProvider,
        moduleDataProvider: ModuleDataProvider,
        configuration: CompilerConfiguration,
        extensionRegistrars: List<FirExtensionRegistrar>,
        predefinedJavaComponents: FirSharableJavaComponents?
    ): Pair<VfsBasedProjectEnvironment?, FirSession> {
        val compilerConfigurationProvider = testServices.compilerConfigurationProvider
        val projectEnvironment: VfsBasedProjectEnvironment?
        val languageVersionSettings = module.languageVersionSettings
        val targetPlatform = module.targetPlatform(testServices)
        val isCommon = targetPlatform.isCommon()
        val session = when {
            isCommon || targetPlatform.isJvm() -> {
                val packagePartProviderFactory = compilerConfigurationProvider.getPackagePartProviderFactory(module)
                projectEnvironment = VfsBasedProjectEnvironment(
                    project, VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL),
                ) { packagePartProviderFactory.invoke(it) }
                val projectFileSearchScope = PsiBasedProjectFileSearchScope(ProjectScope.getLibrariesScope(project))
                val packagePartProvider = projectEnvironment.getPackagePartProvider(projectFileSearchScope)

                if (isCommon) {
                    val klibFiles = configuration.get(CLIConfigurationKeys.CONTENT_ROOTS).orEmpty()
                        .filterIsInstance<JvmClasspathRoot>()
                        .filter { it.file.isDirectory || it.file.extension == "klib" }
                        .map { it.file.absolutePath }

                    val resolvedKLibs = klibFiles.map {
                        KotlinResolvedLibraryImpl(resolveSingleFileKlib(KFile(it), configuration.getLogger()))
                    }

                    val sharedLibrarySession = FirMetadataSessionFactory.createSharedLibrarySession(
                        mainModuleName = moduleName,
                        sessionProvider = sessionProvider,
                        moduleDataProvider = moduleDataProvider,
                        languageVersionSettings = languageVersionSettings,
                        extensionRegistrars = extensionRegistrars,
                    )

                    FirMetadataSessionFactory.createLibrarySession(
                        sessionProvider = sessionProvider,
                        sharedLibrarySession,
                        moduleDataProvider = moduleDataProvider,
                        projectEnvironment = projectEnvironment,
                        extensionRegistrars = extensionRegistrars,
                        librariesScope = projectFileSearchScope,
                        resolvedKLibs = resolvedKLibs,
                        packageAndMetadataPartProvider = packagePartProvider as PackageAndMetadataPartProvider,
                        languageVersionSettings = languageVersionSettings,
                    ).also(::registerExtraComponents)
                } else {
                    val sharedLibrarySession = FirJvmSessionFactory.createSharedLibrarySession(
                        moduleName,
                        sessionProvider,
                        moduleDataProvider,
                        projectEnvironment,
                        extensionRegistrars,
                        projectFileSearchScope,
                        packagePartProvider,
                        languageVersionSettings,
                        predefinedJavaComponents,
                    )

                    FirJvmSessionFactory.createLibrarySession(
                        sessionProvider,
                        sharedLibrarySession,
                        moduleDataProvider,
                        projectEnvironment,
                        extensionRegistrars,
                        projectFileSearchScope,
                        packagePartProvider,
                        languageVersionSettings,
                        predefinedJavaComponents,
                    ).also(::registerExtraComponents)
                }
            }
            targetPlatform.isJs() -> {
                projectEnvironment = null
                TestFirJsSessionFactory.createLibrarySession(
                    moduleName,
                    sessionProvider,
                    moduleDataProvider,
                    module,
                    testServices,
                    configuration,
                    extensionRegistrars,
                ).also(::registerExtraComponents)
            }
            targetPlatform.isNative() -> {
                projectEnvironment = null
                TestFirNativeSessionFactory.createLibrarySession(
                    moduleName,
                    module,
                    testServices,
                    sessionProvider,
                    moduleDataProvider,
                    configuration,
                    extensionRegistrars,
                ).also(::registerExtraComponents)
            }
            targetPlatform.isWasm() -> {
                projectEnvironment = null
                TestFirWasmSessionFactory.createLibrarySession(
                    moduleName,
                    sessionProvider,
                    moduleDataProvider,
                    module,
                    testServices,
                    configuration,
                    extensionRegistrars,
                ).also(::registerExtraComponents)
            }
            else -> error("Unsupported")
        }
        return projectEnvironment to session
    }

    private fun analyze(
        module: TestModule,
        moduleData: FirModuleData,
        targetPlatform: TargetPlatform,
        projectEnvironment: VfsBasedProjectEnvironment?,
        librarySession: FirSession,
        extensionRegistrars: List<FirExtensionRegistrar>,
        predefinedJavaComponents: FirSharableJavaComponents?,
    ): FirOutputPartForDependsOnModule {
        val compilerConfigurationProvider = testServices.compilerConfigurationProvider
        val moduleInfoProvider = testServices.firModuleInfoProvider
        val sessionProvider = moduleInfoProvider.firSessionProvider

        val project = compilerConfigurationProvider.getProject(module)

        PsiElementFinder.EP.getPoint(project).unregisterFinders<JavaElementFinder>()

        val parser = module.directives.singleValue(FirDiagnosticsDirectives.FIR_PARSER)

        val (ktFiles, lightTreeFiles) = when (parser) {
            FirParser.LightTree -> {
                emptyMap<TestFile, KtFile>() to testServices.sourceFileProvider.getKtSourceFilesForSourceFiles(module.files)
            }
            FirParser.Psi -> testServices.sourceFileProvider.getKtFilesForSourceFiles(module.files, project) to emptyMap()
        }

        val sessionConfigurator: FirSessionConfigurator.() -> Unit = {
            registerComponent(FirBuiltinSyntheticFunctionInterfaceProvider::class, librarySession.syntheticFunctionInterfacesSymbolProvider)

            if (FirDiagnosticsDirectives.WITH_EXTRA_CHECKERS in module.directives) {
                registerExtraCommonCheckers()
            }
            if (FirDiagnosticsDirectives.WITH_EXPERIMENTAL_CHECKERS in module.directives) {
                registerExperimentalCheckers()
            }
        }

        val moduleBasedSession = createModuleBasedSession(
            module,
            moduleData,
            targetPlatform,
            sessionProvider,
            projectEnvironment,
            extensionRegistrars,
            sessionConfigurator,
            predefinedJavaComponents,
            project,
            ktFiles.values
        )

        val firAnalyzerFacade = FirAnalyzerFacade(
            moduleBasedSession,
            ktFiles.values,
            lightTreeFiles.values,
            parser,
            testServices.firDiagnosticCollectorService.reporterForLTSyntaxErrors
        )
        val firFiles = firAnalyzerFacade.runResolution()

        val usedFilesMap = when (parser) {
            FirParser.LightTree -> lightTreeFiles
            FirParser.Psi -> ktFiles
        }

        val filesMap = usedFilesMap.keys
            .zip(firFiles)
            .onEach { assert(it.first.name == it.second.name) }
            .toMap()

        return FirOutputPartForDependsOnModule(
            module,
            moduleBasedSession,
            firAnalyzerFacade.scopeSession,
            firAnalyzerFacade,
            filesMap
        )
    }

    private fun createModuleBasedSession(
        module: TestModule,
        moduleData: FirModuleData,
        targetPlatform: TargetPlatform,
        sessionProvider: FirProjectSessionProvider,
        projectEnvironment: VfsBasedProjectEnvironment?,
        extensionRegistrars: List<FirExtensionRegistrar>,
        sessionConfigurator: FirSessionConfigurator.() -> Unit,
        predefinedJavaComponents: FirSharableJavaComponents?,
        project: Project,
        ktFiles: Collection<KtFile>,
    ): FirSession {
        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
        return when {
            targetPlatform.isCommon() -> {
                FirMetadataSessionFactory.createSourceSession(
                    moduleData = moduleData,
                    sessionProvider = sessionProvider,
                    projectEnvironment = projectEnvironment!!,
                    incrementalCompilationContext = null,
                    extensionRegistrars = extensionRegistrars,
                    configuration = configuration,
                    init = sessionConfigurator,
                ).also(::registerExtraComponents)
            }
            targetPlatform.isJvm() -> {
                FirJvmSessionFactory.createSourceSession(
                    moduleData,
                    sessionProvider,
                    PsiBasedProjectFileSearchScope(TopDownAnalyzerFacadeForJVM.newModuleSearchScope(project, ktFiles)),
                    projectEnvironment!!,
                    createIncrementalCompilationSymbolProviders = { null },
                    extensionRegistrars,
                    configuration,
                    predefinedJavaComponents,
                    needRegisterJavaElementFinder = true,
                    init = sessionConfigurator,
                ).also(::registerExtraComponents)
            }
            targetPlatform.isJs() -> {
                TestFirJsSessionFactory.createModuleBasedSession(
                    moduleData,
                    sessionProvider,
                    extensionRegistrars,
                    configuration,
                    sessionConfigurator,
                ).also(::registerExtraComponents)
            }
            targetPlatform.isNative() -> {
                FirNativeSessionFactory.createSourceSession(
                    moduleData,
                    sessionProvider,
                    extensionRegistrars,
                    configuration,
                    init = sessionConfigurator
                ).also(::registerExtraComponents)
            }
            targetPlatform.isWasm() -> {
                TestFirWasmSessionFactory.createModuleBasedSession(
                    moduleData,
                    sessionProvider,
                    extensionRegistrars,
                    configuration,
                    sessionConfigurator,
                ).also(::registerExtraComponents)
            }
            else -> error("Unsupported")
        }
    }

    companion object {
        fun initializeLibraryList(
            mainModule: TestModule,
            binaryModuleData: BinaryModuleData,
            targetPlatform: TargetPlatform,
            configuration: CompilerConfiguration,
            testServices: TestServices
        ): DependencyListForCliModule {
            return DependencyListForCliModule.build(binaryModuleData) {
                when {
                    targetPlatform.isCommon() || targetPlatform.isJvm() -> {
                        dependencies(configuration.jvmModularRoots.map { it.absolutePath })
                        dependencies(configuration.jvmClasspathRoots.map { it.absolutePath })
                        friendDependencies(configuration[JVMConfigurationKeys.FRIEND_PATHS] ?: emptyList())
                    }
                    targetPlatform.isJs() -> {
                        val runtimeKlibsPaths = JsEnvironmentConfigurator.getRuntimePathsForModule(mainModule, testServices)
                        val (transitiveLibraries, friendLibraries) = getTransitivesAndFriends(mainModule, testServices)
                        dependencies(runtimeKlibsPaths)
                        dependencies(transitiveLibraries.map { it.absolutePath })
                        friendDependencies(friendLibraries.map { it.absolutePath })
                    }
                    targetPlatform.isNative() -> {
                        val runtimeKlibsPaths = NativeEnvironmentConfigurator.getRuntimePathsForModule(mainModule, testServices)
                        val (transitiveLibraries, friendLibraries) = getTransitivesAndFriends(mainModule, testServices)
                        dependencies(runtimeKlibsPaths)
                        dependencies(transitiveLibraries.map { it.absolutePath })
                        friendDependencies(friendLibraries.map { it.absolutePath })
                    }
                    targetPlatform.isWasm() -> {
                        val runtimeKlibsPaths = WasmEnvironmentConfigurator.getRuntimePathsForModule(
                            configuration.get(WasmConfigurationKeys.WASM_TARGET, WasmTarget.JS)
                        )
                        val (transitiveLibraries, friendLibraries) = getTransitivesAndFriends(mainModule, testServices)
                        dependencies(runtimeKlibsPaths)
                        dependencies(transitiveLibraries.map { it.absolutePath })
                        friendDependencies(friendLibraries.map { it.absolutePath })
                    }
                    else -> error("Unsupported")
                }
            }
        }

        fun shouldRunFirFrontendFacade(
            module: TestModule,
            testServices: TestServices,
        ): Boolean {
            val shouldRunAnalysis = testServices.defaultsProvider.frontendKind == FrontendKinds.FIR

            if (!shouldRunAnalysis) {
                return false
            }

            return if (module.languageVersionSettings.supportsFeature(MultiPlatformProjects)) {
                module.isLeafModuleInMppGraph(testServices)
            } else {
                true
            }
        }

    }
}
