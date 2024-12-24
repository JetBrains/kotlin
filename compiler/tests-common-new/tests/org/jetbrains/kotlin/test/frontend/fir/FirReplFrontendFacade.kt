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
import org.jetbrains.kotlin.cli.jvm.compiler.*
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.jvmModularRoots
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageFeature.MultiPlatformProjects
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.checkers.registerExperimentalCheckers
import org.jetbrains.kotlin.fir.checkers.registerExtraCommonCheckers
import org.jetbrains.kotlin.fir.deserialization.ModuleDataProvider
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.extensions.FirReplHistoryProvider
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.fir.session.*
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectEnvironment
import org.jetbrains.kotlin.fir.symbols.impl.FirReplSnippetSymbol
import org.jetbrains.kotlin.library.metadata.resolver.impl.KotlinResolvedLibraryImpl
import org.jetbrains.kotlin.library.resolveSingleFileKlib
import org.jetbrains.kotlin.load.kotlin.PackageAndMetadataPartProvider
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.Name.special
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.platform.isJs
import org.jetbrains.kotlin.platform.isWasm
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.platform.konan.isNative
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.singleValue
import org.jetbrains.kotlin.test.model.FrontendFacade
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.runners.lightTreeSyntaxDiagnosticsReporterHolder
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.wasm.config.wasmTarget
import org.jetbrains.kotlin.konan.file.File as KFile

open class FirReplFrontendFacade(
    testServices: TestServices,
    private val additionalSessionConfiguration: SessionConfiguration?
) : FrontendFacade<FirOutputArtifact>(testServices, FrontendKinds.FIR) {
    // Separate constructor is needed for creating callable references to it
    constructor(testServices: TestServices) : this(testServices, additionalSessionConfiguration = null)

    fun interface SessionConfiguration : (FirSessionConfigurator) -> Unit

    override val additionalServices: List<ServiceRegistrationData>
        get() = listOf(service(::FirModuleInfoProvider))

    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(FirDiagnosticsDirectives)

    override fun shouldTransform(module: TestModule): Boolean {
        if (!super.shouldTransform(module)) return false

        return if (module.languageVersionSettings.supportsFeature(LanguageFeature.MultiPlatformProjects)) {
            module.isLeafModuleInMppGraph(testServices)
        } else {
            true
        }
    }

    private fun registerExtraComponents(session: FirSession) {
        testServices.firSessionComponentRegistrar?.registerAdditionalComponent(session)
    }

    private class ReplCompilationEnvironment(
        val targetPlatform: TargetPlatform,
        val extensionRegistrars: List<FirExtensionRegistrar>,
        val predefinedJavaComponents: FirSharableJavaComponents?,
        val projectEnvironment: AbstractProjectEnvironment?,
        val libraryList: DependencyListForCliModule
    )

    @OptIn(org.jetbrains.kotlin.fir.SessionConfiguration::class)
    private val replCompilationEnvironment: ReplCompilationEnvironment by lazy {
        val firstSnippetModule = testServices.moduleStructure.modules.first()
        val project = this.testServices.compilerConfigurationProvider.getProject(firstSnippetModule)
        val configuration = this.testServices.compilerConfigurationProvider.getCompilerConfiguration(firstSnippetModule)
        val libraryList = createLibraryListForJvm("repl", configuration, emptyList())
        val extensionRegistrars = FirExtensionRegistrar.getInstances(project)
        val targetPlatform = firstSnippetModule.targetPlatform
        val predefinedJavaComponents = runIf(targetPlatform.isJvm()) {
            FirSharableJavaComponents(firCachesFactoryForCliMode)
        }
        val (librarySession, projectEnvironment) = createLibrarySessionAndProjectEnvironment(
            firstSnippetModule,
            project,
            special("<${firstSnippetModule.name}>"),
            this.testServices.firModuleInfoProvider.firSessionProvider,
            libraryList.moduleDataProvider,
            this.testServices.compilerConfigurationProvider.getCompilerConfiguration(firstSnippetModule),
            extensionRegistrars,
            predefinedJavaComponents
        )
        librarySession.register(FirReplHistoryProvider::class, FirReplHistoryProviderImpl())
        ReplCompilationEnvironment(
            targetPlatform,
            extensionRegistrars,
            predefinedJavaComponents,
            projectEnvironment,
            libraryList
        )
    }

    override fun analyze(module: TestModule): FirOutputArtifact {
        val moduleDataMap = initializeModuleData(listOf(module))

        val firOutputPartForDependsOnModules = with(replCompilationEnvironment) {
            listOf(
                analyze(module, moduleDataMap[module]!!, targetPlatform, projectEnvironment, extensionRegistrars, predefinedJavaComponents)
            )
        }

        return FirOutputArtifactImpl(firOutputPartForDependsOnModules)
    }

    private fun initializeModuleData(modules: List<TestModule>): Map<TestModule, FirModuleData> {
        val moduleInfoProvider = testServices.firModuleInfoProvider
        val moduleDataMap = mutableMapOf<TestModule, FirModuleData>()

        for (module in modules) {
            with(replCompilationEnvironment) {
                val regularModules = libraryList.regularDependencies + moduleInfoProvider.getRegularDependentSourceModules(module)
                // TODO: collect instead of recursive traversal on each new snippet
                val friendModules = libraryList.friendsDependencies + moduleInfoProvider.getDependentFriendSourceModulesRecursively(module)
                val dependsOnModules = libraryList.dependsOnDependencies + moduleInfoProvider.getDependentDependsOnSourceModules(module)

                val moduleData = FirModuleDataImpl(
                    Name.special("<${module.name}>"),
                    regularModules,
                    dependsOnModules,
                    friendModules,
                    replCompilationEnvironment.targetPlatform,
                    isCommon = module.languageVersionSettings.supportsFeature(MultiPlatformProjects) && !module.isLeafModuleInMppGraph(testServices),
                )

                moduleInfoProvider.registerModuleData(module, moduleData)

                moduleDataMap[module] = moduleData
            }
        }

        return moduleDataMap
    }

    private fun createLibrarySessionAndProjectEnvironment(
        module: TestModule,
        project: Project,
        moduleName: Name,
        sessionProvider: FirProjectSessionProvider,
        moduleDataProvider: ModuleDataProvider,
        configuration: CompilerConfiguration,
        extensionRegistrars: List<FirExtensionRegistrar>,
        predefinedJavaComponents: FirSharableJavaComponents?
    ): Pair<FirSession, VfsBasedProjectEnvironment?> {
        val compilerConfigurationProvider = testServices.compilerConfigurationProvider
        val projectEnvironment: AbstractProjectEnvironment?
        val languageVersionSettings = module.languageVersionSettings
        val isCommon = module.targetPlatform.isCommon()
        val session = when {
            isCommon || module.targetPlatform.isJvm() -> {
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

                    FirCommonSessionFactory.createLibrarySession(
                        mainModuleName = moduleName,
                        sessionProvider = sessionProvider,
                        moduleDataProvider = moduleDataProvider,
                        projectEnvironment = projectEnvironment,
                        extensionRegistrars = extensionRegistrars,
                        librariesScope = projectFileSearchScope,
                        resolvedKLibs = resolvedKLibs,
                        packageAndMetadataPartProvider = packagePartProvider as PackageAndMetadataPartProvider,
                        languageVersionSettings = languageVersionSettings,
                    ).also(::registerExtraComponents)
                } else {
                    FirJvmSessionFactory.createLibrarySession(
                        moduleName,
                        sessionProvider,
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
            module.targetPlatform.isJs() -> {
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
            module.targetPlatform.isNative() -> {
                projectEnvironment = null
                TestFirNativeSessionFactory.createLibrarySession(
                    moduleName,
                    module,
                    testServices,
                    sessionProvider,
                    moduleDataProvider,
                    configuration,
                    extensionRegistrars,
                    languageVersionSettings,
                ).also(::registerExtraComponents)
            }
            module.targetPlatform.isWasm() -> {
                projectEnvironment = null
                TestFirWasmSessionFactory.createLibrarySession(
                    moduleName,
                    sessionProvider,
                    moduleDataProvider,
                    module,
                    testServices,
                    configuration,
                    extensionRegistrars,
                    languageVersionSettings,
                ).also(::registerExtraComponents)
            }
            else -> error("Unsupported")
        }
        return session to projectEnvironment
    }

    private fun analyze(
        module: TestModule,
        moduleData: FirModuleData,
        targetPlatform: TargetPlatform,
        projectEnvironment: AbstractProjectEnvironment?,
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
            if (FirDiagnosticsDirectives.WITH_EXTRA_CHECKERS in module.directives) {
                registerExtraCommonCheckers()
            }
            if (FirDiagnosticsDirectives.WITH_EXPERIMENTAL_CHECKERS in module.directives) {
                registerExperimentalCheckers()
            }
            additionalSessionConfiguration?.invoke(this)
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
            testServices.lightTreeSyntaxDiagnosticsReporterHolder?.reporter,
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

        return FirOutputPartForDependsOnModule(module, moduleBasedSession, firAnalyzerFacade, filesMap)
    }

    private fun createModuleBasedSession(
        module: TestModule,
        moduleData: FirModuleData,
        targetPlatform: TargetPlatform,
        sessionProvider: FirProjectSessionProvider,
        projectEnvironment: AbstractProjectEnvironment?,
        extensionRegistrars: List<FirExtensionRegistrar>,
        sessionConfigurator: FirSessionConfigurator.() -> Unit,
        predefinedJavaComponents: FirSharableJavaComponents?,
        project: Project,
        ktFiles: Collection<KtFile>,
    ): FirSession {
        val languageVersionSettings = module.languageVersionSettings
        return when {
            targetPlatform.isCommon() -> {
                FirCommonSessionFactory.createModuleBasedSession(
                    moduleData = moduleData,
                    sessionProvider = sessionProvider,
                    projectEnvironment = projectEnvironment!!,
                    incrementalCompilationContext = null,
                    extensionRegistrars = extensionRegistrars,
                    languageVersionSettings = languageVersionSettings,
                    init = sessionConfigurator,
                ).also(::registerExtraComponents)
            }
            targetPlatform.isJvm() -> {
                FirJvmSessionFactory.createModuleBasedSession(
                    moduleData,
                    sessionProvider,
                    PsiBasedProjectFileSearchScope(TopDownAnalyzerFacadeForJVM.newModuleSearchScope(project, ktFiles)),
                    projectEnvironment!!,
                    createIncrementalCompilationSymbolProviders = { null },
                    extensionRegistrars,
                    languageVersionSettings,
                    jvmTarget = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
                        .get(JVMConfigurationKeys.JVM_TARGET, JvmTarget.DEFAULT),
                    lookupTracker = null,
                    enumWhenTracker = null,
                    importTracker = null,
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
                    testServices.compilerConfigurationProvider.getCompilerConfiguration(module),
                    lookupTracker = null,
                    sessionConfigurator,
                ).also(::registerExtraComponents)
            }
            targetPlatform.isNative() -> {
                FirNativeSessionFactory.createModuleBasedSession(
                    moduleData,
                    sessionProvider,
                    extensionRegistrars,
                    languageVersionSettings,
                    init = sessionConfigurator
                ).also(::registerExtraComponents)
            }
            targetPlatform.isWasm() -> {
                TestFirWasmSessionFactory.createModuleBasedSession(
                    moduleData,
                    sessionProvider,
                    extensionRegistrars,
                    languageVersionSettings,
                    testServices.compilerConfigurationProvider.getCompilerConfiguration(module).wasmTarget,
                    lookupTracker = null,
                    sessionConfigurator,
                ).also(::registerExtraComponents)
            }
            else -> error("Unsupported")
        }
    }

    private class FirReplHistoryProviderImpl : FirReplHistoryProvider() {
        private val history = LinkedHashSet<FirReplSnippetSymbol>()

        override fun getSnippets(): Iterable<FirReplSnippetSymbol> = history.asIterable()

        override fun putSnippet(symbol: FirReplSnippetSymbol) {
            history.add(symbol)
        }

    }

    companion object {
        fun initializeLibraryList(
            binaryModuleData: BinaryModuleData,
            configuration: CompilerConfiguration
        ): DependencyListForCliModule {
            return DependencyListForCliModule.build(binaryModuleData) {
                dependencies(configuration.jvmModularRoots.map { it.toPath() })
                dependencies(configuration.jvmClasspathRoots.map { it.toPath() })
                friendDependencies(configuration[JVMConfigurationKeys.FRIEND_PATHS] ?: emptyList())
            }
        }
    }
}
