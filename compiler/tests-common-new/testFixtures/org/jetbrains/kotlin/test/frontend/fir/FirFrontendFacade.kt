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
import org.jetbrains.kotlin.backend.common.loadMetadataKlibs
import org.jetbrains.kotlin.backend.konan.serialization.loadNativeKlibsInTestPipeline
import org.jetbrains.kotlin.cli.common.contentRoots
import org.jetbrains.kotlin.cli.jvm.compiler.PsiBasedProjectFileSearchScope
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.unregisterFinders
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.jvmModularRoots
import org.jetbrains.kotlin.compiler.plugin.getCompilerExtensions
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.LanguageFeature.MultiPlatformProjects
import org.jetbrains.kotlin.config.targetPlatform
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.checkers.registerExperimentalCheckers
import org.jetbrains.kotlin.fir.checkers.registerExtraCommonCheckers
import org.jetbrains.kotlin.fir.deserialization.ModuleDataProvider
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.resolve.ImplicitIntegerCoercionModuleCapability
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirBuiltinSyntheticFunctionInterfaceProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.syntheticFunctionInterfacesSymbolProvider
import org.jetbrains.kotlin.fir.session.*
import org.jetbrains.kotlin.fir.session.AbstractFirMetadataSessionFactory.JarMetadataProviderComponents
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.metadata.isCInteropLibrary
import org.jetbrains.kotlin.load.kotlin.PackageAndMetadataPartProvider
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.*
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
import org.jetbrains.kotlin.test.services.configuration.nativeEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.WasmEnvironmentConfigurator
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.wasm.config.WasmConfigurationKeys

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
        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
        val extensionRegistrars = configuration.getCompilerExtensions(FirExtensionRegistrar)
        val targetPlatform = module.targetPlatform(testServices)
        val jvmSessionFactoryContext = runIf(targetPlatform.isCommon() || targetPlatform.isJvm()) {
            val packagePartProviderFactory = testServices.compilerConfigurationProvider.getPackagePartProviderFactory(module)
            val projectEnvironment = VfsBasedProjectEnvironment(
                project, VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL),
            ) { packagePartProviderFactory.invoke(it) }
            val librariesScope = PsiBasedProjectFileSearchScope(ProjectScope.getLibrariesScope(project))
            FirJvmSessionFactory.Context(
                configuration,
                projectEnvironment,
                librariesScope,
            )
        }
        val librarySession = createLibrarySession(
            module,
            Name.special("<${module.name}>"),
            moduleDataProvider,
            configuration,
            extensionRegistrars,
            jvmSessionFactoryContext,
        )

        val firOutputPartForDependsOnModules = sortedModules.map {
            analyze(it, moduleDataMap[it]!!, targetPlatform, librarySession, extensionRegistrars, jvmSessionFactoryContext)
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

        val compilerConfigurationProvider = testServices.compilerConfigurationProvider
        val configuration = compilerConfigurationProvider.getCompilerConfiguration(mainModule)

        val libraryList = initializeLibraryList(mainModule, moduleName, targetPlatform, configuration, testServices)

        val moduleInfoProvider = testServices.firModuleInfoProvider
        val moduleDataMap = mutableMapOf<TestModule, FirModuleData>()

        for (module in modules) {
            val regularModules = libraryList.regularDependencies + moduleInfoProvider.getRegularDependentSourceModules(module)
            val friendModules = libraryList.friendDependencies + moduleInfoProvider.getDependentFriendSourceModules(module)
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
        moduleName: Name,
        moduleDataProvider: ModuleDataProvider,
        configuration: CompilerConfiguration,
        extensionRegistrars: List<FirExtensionRegistrar>,
        jvmSessionFactoryContext: FirJvmSessionFactory.Context?
    ): FirSession {
        val languageVersionSettings = module.languageVersionSettings
        val targetPlatform = module.targetPlatform(testServices)
        val isCommon = targetPlatform.isCommon()
        return when {
            isCommon || targetPlatform.isJvm() -> {
                requireNotNull(jvmSessionFactoryContext)

                if (isCommon) {
                    val klibs: List<KotlinLibrary> = loadMetadataKlibs(
                        libraryPaths = configuration.contentRoots.mapNotNull { (it as? JvmClasspathRoot)?.file?.path },
                        configuration = configuration,
                    ).all
                    val context = AbstractFirMetadataSessionFactory.Context(
                        createJvmContext = { jvmSessionFactoryContext },
                        createJsContext = { FirJsSessionFactory.Context(configuration) },
                    )
                    val sessionFactory = FirMetadataSessionFactory(configuration.targetPlatform ?: CommonPlatforms.defaultCommonPlatform)
                    val sharedLibrarySession = sessionFactory.createSharedLibrarySession(
                        mainModuleName = moduleName,
                        languageVersionSettings = languageVersionSettings,
                        extensionRegistrars = extensionRegistrars,
                        context = context,
                    )

                    sessionFactory.createLibrarySession(
                        sharedLibrarySession,
                        moduleDataProvider = moduleDataProvider,
                        extensionRegistrars = extensionRegistrars,
                        JarMetadataProviderComponents(
                            jvmSessionFactoryContext.packagePartProviderForLibraries as PackageAndMetadataPartProvider,
                            jvmSessionFactoryContext.librariesScope,
                            jvmSessionFactoryContext.projectEnvironment,
                        ),
                        resolvedKLibs = klibs,
                        languageVersionSettings = languageVersionSettings,
                        context = context
                    ).also(::registerExtraComponents)
                } else {
                    val sharedLibrarySession = FirJvmSessionFactory.createSharedLibrarySession(
                        moduleName,
                        extensionRegistrars,
                        languageVersionSettings,
                        jvmSessionFactoryContext,
                    )

                    FirJvmSessionFactory.createLibrarySession(
                        sharedLibrarySession,
                        moduleDataProvider,
                        extensionRegistrars,
                        languageVersionSettings,
                        jvmSessionFactoryContext,
                    ).also(::registerExtraComponents)
                }
            }
            targetPlatform.isJs() -> {
                TestFirJsSessionFactory.createLibrarySession(
                    moduleName,
                    moduleDataProvider,
                    module,
                    testServices,
                    configuration,
                    extensionRegistrars,
                ).also(::registerExtraComponents)
            }
            targetPlatform.isNative() -> {
                TestFirNativeSessionFactory.createLibrarySession(
                    moduleName,
                    module,
                    testServices,
                    moduleDataProvider,
                    configuration,
                    extensionRegistrars,
                ).also(::registerExtraComponents)
            }
            targetPlatform.isWasm() -> {
                TestFirWasmSessionFactory.createLibrarySession(
                    moduleName,
                    moduleDataProvider,
                    module,
                    testServices,
                    configuration,
                    extensionRegistrars,
                ).also(::registerExtraComponents)
            }
            else -> error("Unsupported")
        }
    }

    private fun analyze(
        module: TestModule,
        moduleData: FirModuleData,
        targetPlatform: TargetPlatform,
        librarySession: FirSession,
        extensionRegistrars: List<FirExtensionRegistrar>,
        jvmSessionFactoryContext: FirJvmSessionFactory.Context?,
    ): FirOutputPartForDependsOnModule {
        val compilerConfigurationProvider = testServices.compilerConfigurationProvider

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
            extensionRegistrars,
            sessionConfigurator,
            jvmSessionFactoryContext,
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
        extensionRegistrars: List<FirExtensionRegistrar>,
        sessionConfigurator: FirSessionConfigurator.() -> Unit,
        jvmSessionFactoryContext: FirJvmSessionFactory.Context?,
        project: Project,
        ktFiles: Collection<KtFile>,
    ): FirSession {
        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
        val sessionFactory = FirMetadataSessionFactory(configuration.targetPlatform ?: CommonPlatforms.defaultCommonPlatform)
        return when {
            targetPlatform.isCommon() -> {
                sessionFactory.createSourceSession(
                    moduleData = moduleData,
                    projectEnvironment = jvmSessionFactoryContext!!.projectEnvironment,
                    incrementalCompilationContext = null,
                    extensionRegistrars = extensionRegistrars,
                    configuration = configuration,
                    context = AbstractFirMetadataSessionFactory.Context(
                        createJvmContext = { jvmSessionFactoryContext },
                        createJsContext = { FirJsSessionFactory.Context(configuration) }
                    ),
                    isForLeafHmppModule = false,
                    init = sessionConfigurator,
                ).also(::registerExtraComponents)
            }
            targetPlatform.isJvm() -> {
                FirJvmSessionFactory.createSourceSession(
                    moduleData,
                    PsiBasedProjectFileSearchScope(TopDownAnalyzerFacadeForJVM.newModuleSearchScope(project, ktFiles)),
                    createIncrementalCompilationSymbolProviders = { null },
                    extensionRegistrars,
                    configuration,
                    jvmSessionFactoryContext!!,
                    needRegisterJavaElementFinder = true,
                    isForLeafHmppModule = false,
                    init = sessionConfigurator,
                ).also(::registerExtraComponents)
            }
            targetPlatform.isJs() -> {
                TestFirJsSessionFactory.createModuleBasedSession(
                    moduleData,
                    extensionRegistrars,
                    configuration,
                    sessionConfigurator,
                ).also(::registerExtraComponents)
            }
            targetPlatform.isNative() -> {
                FirNativeSessionFactory.createSourceSession(
                    moduleData,
                    extensionRegistrars,
                    configuration,
                    isForLeafHmppModule = false,
                    init = sessionConfigurator
                ).also(::registerExtraComponents)
            }
            targetPlatform.isWasm() -> {
                TestFirWasmSessionFactory.createModuleBasedSession(
                    moduleData,
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
            mainModuleName: Name,
            targetPlatform: TargetPlatform,
            configuration: CompilerConfiguration,
            testServices: TestServices
        ): DependencyListForCliModule {
            return DependencyListForCliModule.build {
                defaultDependenciesSet(mainModuleName) {
                    when {
                        targetPlatform.isCommon() || targetPlatform.isJvm() -> {
                            dependencies(configuration.jvmModularRoots.map { it.path })
                            dependencies(configuration.jvmClasspathRoots.map { it.path })
                            friendDependencies(configuration[JVMConfigurationKeys.FRIEND_PATHS] ?: emptyList())
                        }
                        targetPlatform.isJs() -> {
                            val runtimeKlibsPaths = JsEnvironmentConfigurator.getRuntimePathsForModule(mainModule, testServices)
                            val (transitiveLibraries, friendLibraries) = getTransitivesAndFriends(mainModule, testServices)
                            dependencies(runtimeKlibsPaths)
                            dependencies(transitiveLibraries.map { it.path })
                            friendDependencies(friendLibraries.map { it.path })
                        }
                        targetPlatform.isNative() -> {
                            val nativeEnvironmentConfigurator = testServices.nativeEnvironmentConfigurator
                            val runtimeLibraryProviders = nativeEnvironmentConfigurator.getRuntimeLibraryProviders(mainModule)

                            val (transitiveLibraries, friendLibraries) = getTransitivesAndFriends(mainModule, testServices)
                            val allPaths = (runtimeLibraryProviders.flatMap { it.getLibraryPaths() } + transitiveLibraries.map { it.path }).distinct()
                            val friendPaths = friendLibraries.map { it.path }

                            val loadedKlibs = loadNativeKlibsInTestPipeline(
                                configuration,
                                allPaths,
                                friendPaths,
                                nativeTarget = nativeEnvironmentConfigurator.getNativeTarget(mainModule)
                            )

                            val (interopLibs, regularLibs) = loadedKlibs.all.partition { it.isCInteropLibrary() }

                            dependencies(regularLibs.map { it.libraryFile.absolutePath })
                            friendDependencies(friendPaths)

                            if (interopLibs.isNotEmpty()) {
                                val interopModuleData = FirBinaryDependenciesModuleData(
                                    Name.special("<regular interop dependencies of $mainModuleName>"),
                                    FirModuleCapabilities.create(listOf(ImplicitIntegerCoercionModuleCapability))
                                )
                                this@build.dependencies(interopModuleData, interopLibs.map { it.libraryFile.absolutePath })
                            }
                        }
                        targetPlatform.isWasm() -> {
                            val runtimeKlibsPaths = WasmEnvironmentConfigurator.getRuntimePathsForModule(
                                configuration.get(WasmConfigurationKeys.WASM_TARGET, WasmTarget.JS),
                                testServices
                            )
                            val (transitiveLibraries, friendLibraries) = getTransitivesAndFriends(mainModule, testServices)
                            dependencies(runtimeKlibsPaths)
                            dependencies(transitiveLibraries.map { it.path })
                            friendDependencies(friendLibraries.map { it.path })
                        }
                        else -> error("Unsupported")
                    }
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
