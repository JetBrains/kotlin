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
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.jvm.compiler.PsiBasedProjectFileSearchScope
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.jvmModularRoots
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.checkers.registerExtendedCommonCheckers
import org.jetbrains.kotlin.fir.deserialization.ModuleDataProvider
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.fir.session.FirJvmSessionFactory
import org.jetbrains.kotlin.fir.session.FirNativeSessionFactory
import org.jetbrains.kotlin.fir.session.FirSessionConfigurator
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectEnvironment
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.platform.isJs
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.platform.konan.isNative
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.getAnalyzerServices
import org.jetbrains.kotlin.test.model.FrontendFacade
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*
import java.nio.file.Paths

open class FirFrontendFacade(
    testServices: TestServices,
    private val additionalSessionConfiguration: SessionConfiguration?
) : FrontendFacade<FirOutputArtifact>(testServices, FrontendKinds.FIR) {
    private val testModulesByName by lazy { testServices.moduleStructure.modules.associateBy { it.name } }

    // Separate constructor is needed for creating callable references to it
    constructor(testServices: TestServices) : this(testServices, additionalSessionConfiguration = null)

    fun interface SessionConfiguration : (FirSessionConfigurator) -> Unit

    override val additionalServices: List<ServiceRegistrationData>
        get() = listOf(service(::FirModuleInfoProvider))

    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(FirDiagnosticsDirectives)

    override fun shouldRunAnalysis(module: TestModule): Boolean {
        if (!super.shouldRunAnalysis(module)) return false

        return if (module.languageVersionSettings.supportsFeature(LanguageFeature.MultiPlatformProjects)) {
            testServices.moduleStructure
                .modules.none { testModule -> testModule.dependsOnDependencies.any { testModulesByName[it.moduleName] == module } }
        } else {
            true
        }
    }

    fun registerExtraComponents(session: FirSession) {
        testServices.firSessionComponentRegistrar?.registerAdditionalComponent(session)
    }

    override fun analyze(module: TestModule): FirOutputArtifact {
        val isMppSupported = module.languageVersionSettings.supportsFeature(LanguageFeature.MultiPlatformProjects)

        val sortedModules = if (isMppSupported) sortDependsOnTopologically(module) else listOf(module)

        val (moduleDataMap, moduleDataProvider) = initializeModuleData(sortedModules)

        val projectEnvironment = createLibrarySession(
            module,
            testServices.compilerConfigurationProvider.getProject(module),
            Name.special("<${module.name}>"),
            testServices.firModuleInfoProvider.firSessionProvider,
            moduleDataProvider,
            testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
        )

        val targetPlatform = module.targetPlatform
        val firOutputPartForDependsOnModules = mutableListOf<FirOutputPartForDependsOnModule>()
        for (testModule in sortedModules) {
            firOutputPartForDependsOnModules.add(
                analyze(
                    testModule,
                    moduleDataMap[testModule]!!,
                    targetPlatform,
                    projectEnvironment
                )
            )
        }

        return FirOutputArtifactImpl(firOutputPartForDependsOnModules)
    }

    protected fun sortDependsOnTopologically(module: TestModule): List<TestModule> {
        val sortedModules = mutableListOf<TestModule>()
        val visitedModules = mutableSetOf<TestModule>()
        val modulesQueue = ArrayDeque<TestModule>()
        modulesQueue.add(module)

        while (modulesQueue.isNotEmpty()) {
            val currentModule = modulesQueue.removeFirst()
            if (!visitedModules.add(currentModule)) continue
            sortedModules.add(currentModule)

            for (dependency in currentModule.dependsOnDependencies) {
                modulesQueue.add(testServices.dependencyProvider.getTestModule(dependency.moduleName))
            }
        }

        return sortedModules.reversed()
    }

    private fun initializeModuleData(modules: List<TestModule>): Pair<Map<TestModule, FirModuleData>, ModuleDataProvider> {
        val mainModule = modules.last()

        val targetPlatform = mainModule.targetPlatform
        val analyzerServices = targetPlatform.getAnalyzerServices()

        // the special name is required for `KlibMetadataModuleDescriptorFactoryImpl.createDescriptorOptionalBuiltIns`
        // it doesn't seem convincingly legitimate, probably should be refactored
        val moduleName = Name.special("<${mainModule.name}>")
        val binaryModuleData = BinaryModuleData.initialize(moduleName, targetPlatform, analyzerServices)

        val compilerConfigurationProvider = testServices.compilerConfigurationProvider
        val configuration = compilerConfigurationProvider.getCompilerConfiguration(mainModule)

        val libraryList = initializeLibraryList(mainModule, binaryModuleData, targetPlatform, configuration)

        val moduleInfoProvider = testServices.firModuleInfoProvider
        val moduleDataMap = mutableMapOf<TestModule, FirModuleData>()

        for (module in modules) {
            val regularModules = libraryList.regularDependencies + moduleInfoProvider.getRegularDependentSourceModules(module)
            val friendModules = libraryList.friendsDependencies + moduleInfoProvider.getDependentFriendSourceModules(module)
            val dependsOnModules = libraryList.dependsOnDependencies + moduleInfoProvider.getDependentDependsOnSourceModules(module)

            val moduleData = FirModuleDataImpl(
                Name.special("<${module.name}>"),
                regularModules,
                dependsOnModules,
                friendModules,
                module.targetPlatform,
                module.targetPlatform.getAnalyzerServices()
            )

            moduleInfoProvider.registerModuleData(module, moduleData)

            moduleDataMap[module] = moduleData
        }

        return moduleDataMap to libraryList.moduleDataProvider
    }

    private fun initializeLibraryList(
        mainModule: TestModule,
        binaryModuleData: BinaryModuleData,
        targetPlatform: TargetPlatform,
        configuration: CompilerConfiguration,
    ): DependencyListForCliModule {
        return DependencyListForCliModule.build(binaryModuleData) {
            when {
                targetPlatform.isCommon() || targetPlatform.isJvm() || targetPlatform.isNative() -> {
                    dependencies(configuration.jvmModularRoots.map { it.toPath() })
                    dependencies(configuration.jvmClasspathRoots.map { it.toPath() })
                    friendDependencies(configuration[JVMConfigurationKeys.FRIEND_PATHS] ?: emptyList())
                }
                targetPlatform.isJs() -> {
                    val (runtimeKlibsPaths, transitiveLibraries, friendLibraries) = getJsDependencies(mainModule, testServices)
                    dependencies(runtimeKlibsPaths.map { Paths.get(it).toAbsolutePath() })
                    dependencies(transitiveLibraries.map { it.toPath().toAbsolutePath() })
                    friendDependencies(friendLibraries.map { it.toPath().toAbsolutePath() })
                }
                else -> error("Unsupported")
            }
        }
    }

    private fun createLibrarySession(
        module: TestModule,
        project: Project,
        moduleName: Name,
        sessionProvider: FirProjectSessionProvider,
        moduleDataProvider: ModuleDataProvider,
        configuration: CompilerConfiguration
    ): AbstractProjectEnvironment? {
        val compilerConfigurationProvider = testServices.compilerConfigurationProvider
        val projectEnvironment: AbstractProjectEnvironment?
        val languageVersionSettings = module.languageVersionSettings
        when {
            // TODO: use common session for common target platform when it's implemented
            module.targetPlatform.isCommon() || module.targetPlatform.isJvm() -> {
                val packagePartProviderFactory = compilerConfigurationProvider.getPackagePartProviderFactory(module)
                projectEnvironment = VfsBasedProjectEnvironment(
                    project, VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL),
                ) { packagePartProviderFactory.invoke(it) }
                val projectFileSearchScope = PsiBasedProjectFileSearchScope(ProjectScope.getLibrariesScope(project))
                val packagePartProvider = projectEnvironment.getPackagePartProvider(projectFileSearchScope)

                FirJvmSessionFactory.createLibrarySession(
                    moduleName,
                    sessionProvider,
                    moduleDataProvider,
                    projectEnvironment,
                    projectFileSearchScope,
                    packagePartProvider,
                    languageVersionSettings,
                    registerExtraComponents = ::registerExtraComponents,
                )
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
                    languageVersionSettings,
                    registerExtraComponents = ::registerExtraComponents,
                )
            }
            module.targetPlatform.isNative() -> {
                projectEnvironment = null
                FirNativeSessionFactory.createLibrarySession(
                    moduleName,
                    listOf(),
                    sessionProvider,
                    moduleDataProvider,
                    languageVersionSettings,
                    registerExtraComponents = ::registerExtraComponents,
                )
            }
            else -> error("Unsupported")
        }
        return projectEnvironment
    }

    private fun analyze(
        module: TestModule,
        moduleData: FirModuleData,
        targetPlatform: TargetPlatform,
        projectEnvironment: AbstractProjectEnvironment?
    ): FirOutputPartForDependsOnModule {
        val compilerConfigurationProvider = testServices.compilerConfigurationProvider
        val moduleInfoProvider = testServices.firModuleInfoProvider
        val sessionProvider = moduleInfoProvider.firSessionProvider

        val project = compilerConfigurationProvider.getProject(module)

        PsiElementFinder.EP.getPoint(project).unregisterExtension(JavaElementFinder::class.java)

        val lightTreeEnabled = FirDiagnosticsDirectives.USE_LIGHT_TREE in module.directives
        val (ktFiles, lightTreeFiles) = if (lightTreeEnabled) {
            emptyList<KtFile>() to testServices.sourceFileProvider.getLightTreeFilesForSourceFiles(module.files).values
        } else {
            testServices.sourceFileProvider.getKtFilesForSourceFiles(module.files, project).values to emptyList()
        }

        val extensionRegistrars = FirExtensionRegistrar.getInstances(project)
        val sessionConfigurator: FirSessionConfigurator.() -> Unit = {
            if (FirDiagnosticsDirectives.WITH_EXTENDED_CHECKERS in module.directives) {
                registerExtendedCommonCheckers()
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
            project,
            ktFiles
        )

        val enablePluginPhases = FirDiagnosticsDirectives.ENABLE_PLUGIN_PHASES in module.directives
        val firAnalyzerFacade = FirAnalyzerFacade(
            moduleBasedSession,
            module.languageVersionSettings,
            ktFiles,
            lightTreeFiles,
            IrGenerationExtension.getInstances(project),
            lightTreeEnabled,
            enablePluginPhases,
            generateSignatures = module.targetBackend == TargetBackend.JVM_IR_SERIALIZE
        )
        val firFiles = firAnalyzerFacade.runResolution()
        val filesMap = firFiles.mapNotNull { firFile ->
            val testFile = module.files.firstOrNull { it.name == firFile.name } ?: return@mapNotNull null
            testFile to firFile
        }.toMap()

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
        project: Project,
        ktFiles: Collection<KtFile>
    ): FirSession {
        val languageVersionSettings = module.languageVersionSettings
        return when {
            // TODO: use common session for common target platform when it's implemented
            targetPlatform.isCommon() || targetPlatform.isJvm() -> {
                FirJvmSessionFactory.createModuleBasedSession(
                    moduleData,
                    sessionProvider,
                    PsiBasedProjectFileSearchScope(TopDownAnalyzerFacadeForJVM.newModuleSearchScope(project, ktFiles)),
                    projectEnvironment!!,
                    incrementalCompilationContext = null,
                    extensionRegistrars,
                    languageVersionSettings,
                    lookupTracker = null,
                    enumWhenTracker = null,
                    needRegisterJavaElementFinder = true,
                    registerExtraComponents = ::registerExtraComponents,
                    sessionConfigurator,
                )
            }
            targetPlatform.isJs() -> {
                TestFirJsSessionFactory.createModuleBasedSession(
                    moduleData,
                    sessionProvider,
                    extensionRegistrars,
                    languageVersionSettings,
                    null,
                    registerExtraComponents = ::registerExtraComponents,
                    sessionConfigurator,
                )
            }
            targetPlatform.isNative() -> {
                FirNativeSessionFactory.createModuleBasedSession(
                    moduleData,
                    sessionProvider,
                    extensionRegistrars,
                    languageVersionSettings,
                    registerExtraComponents = ::registerExtraComponents,
                    init = sessionConfigurator
                )
            }
            else -> error("Unsupported")
        }
    }
}
