/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import org.jetbrains.kotlin.analyzer.common.CommonPlatformAnalyzerServices
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.jvm.compiler.PsiBasedProjectFileSearchScope
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.jvmModularRoots
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.FirAnalyzerFacade
import org.jetbrains.kotlin.fir.checkers.registerExtendedCommonCheckers
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.fir.session.FirSessionFactory
import org.jetbrains.kotlin.ir.backend.js.jsResolveLibraries
import org.jetbrains.kotlin.ir.backend.js.toResolverLogger
import org.jetbrains.kotlin.ir.util.IrMessageLogger
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.resolve.JsPlatformAnalyzerServices
import org.jetbrains.kotlin.library.resolver.KotlinResolvedLibrary
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.platform.js.isJs
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.platform.konan.isNative
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.PlatformDependentAnalyzerServices
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatformAnalyzerServices
import org.jetbrains.kotlin.resolve.konan.platform.NativePlatformAnalyzerServices
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.model.DependencyRelation
import org.jetbrains.kotlin.test.model.FrontendFacade
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import java.io.File
import java.nio.file.Paths

class FirFrontendFacade(
    testServices: TestServices,
    private val additionalSessionConfiguration: SessionConfiguration?
) : FrontendFacade<FirOutputArtifact>(testServices, FrontendKinds.FIR) {
    // Separate constructor is needed for creating callable references to it
    constructor(testServices: TestServices) : this(testServices, additionalSessionConfiguration = null)

    fun interface SessionConfiguration : (FirSessionFactory.FirSessionConfigurator) -> Unit

    override val additionalServices: List<ServiceRegistrationData>
        get() = listOf(service(::FirModuleInfoProvider))

    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(FirDiagnosticsDirectives)

    override fun analyze(module: TestModule): FirOutputArtifact {
        val moduleInfoProvider = testServices.firModuleInfoProvider
        val compilerConfigurationProvider = testServices.compilerConfigurationProvider
        // TODO: add configurable parser

        val project = compilerConfigurationProvider.getProject(module)

        PsiElementFinder.EP.getPoint(project).unregisterExtension(JavaElementFinder::class.java)

        val lightTreeEnabled = FirDiagnosticsDirectives.USE_LIGHT_TREE in module.directives
        val (ktFiles, lightTreeFiles) = if (lightTreeEnabled) {
            emptyList<KtFile>() to testServices.sourceFileProvider.getLightTreeFilesForSourceFiles(module.files).values
        } else {
            testServices.sourceFileProvider.getKtFilesForSourceFiles(module.files, project).values to emptyList()
        }

        val moduleName = Name.identifier(module.name)
        val languageVersionSettings = module.languageVersionSettings
        val analyzerServices = module.targetPlatform.getAnalyzerServices()
        val configuration = compilerConfigurationProvider.getCompilerConfiguration(module)
        val extensionRegistrars = FirExtensionRegistrar.getInstances(project)

        val sessionConfigurator: FirSessionFactory.FirSessionConfigurator.() -> Unit = {
            if (FirDiagnosticsDirectives.WITH_EXTENDED_CHECKERS in module.directives) {
                registerExtendedCommonCheckers()
            }
            additionalSessionConfiguration?.invoke(this)
        }

        val session = when {
            module.targetPlatform.isJvm() || module.targetPlatform.isCommon() -> {
                configureSessionsForJvmOrCommon(
                    module,
                    moduleName,
                    project,
                    ktFiles,
                    compilerConfigurationProvider,
                    moduleInfoProvider,
                    analyzerServices,
                    configuration,
                    sessionConfigurator,
                    extensionRegistrars,
                    languageVersionSettings,
                )
            }
            module.targetPlatform.isJs() -> {
                configureSessionsForJs(
                    module,
                    moduleName,
                    moduleInfoProvider,
                    analyzerServices,
                    configuration,
                    testServices,
                    sessionConfigurator,
                    extensionRegistrars,
                    languageVersionSettings,
                )
            }
            else -> error("Unsupported")
        }

        moduleInfoProvider.registerModuleData(module, session.moduleData)

        val enablePluginPhases = FirDiagnosticsDirectives.ENABLE_PLUGIN_PHASES in module.directives
        val firAnalyzerFacade = FirAnalyzerFacade(
            session,
            languageVersionSettings,
            ktFiles,
            lightTreeFiles,
            IrGenerationExtension.getInstances(project),
            lightTreeEnabled,
            enablePluginPhases
        )
        val firFiles = firAnalyzerFacade.runResolution()
        val filesMap = firFiles.mapNotNull { firFile ->
            val testFile = module.files.firstOrNull { it.name == firFile.name } ?: return@mapNotNull null
            testFile to firFile
        }.toMap()

        return FirOutputArtifactImpl(session, filesMap, firAnalyzerFacade)
    }
}

private fun DependencyListForCliModule.Builder.configureJvmDependencies(
    configuration: CompilerConfiguration,
) {
    dependencies(configuration.jvmModularRoots.map { it.toPath() })
    dependencies(configuration.jvmClasspathRoots.map { it.toPath() })

    friendDependencies(configuration[JVMConfigurationKeys.FRIEND_PATHS] ?: emptyList())
}

private fun DependencyListForCliModule.Builder.configureJsDependencies(
    module: TestModule,
    testServices: TestServices,
) {
    val (runtimeKlibsPaths, transitiveLibraries, friendLibraries) = getJsDependencies(module, testServices)

    dependencies(runtimeKlibsPaths.map { Paths.get(it).toAbsolutePath() })
    dependencies(transitiveLibraries.map { it.toPath().toAbsolutePath() })

    friendDependencies(friendLibraries.map { it.toPath().toAbsolutePath() })
}

private fun getJsDependencies(module: TestModule, testServices: TestServices): Triple<List<String>, List<File>, List<File>> {
    val runtimeKlibsPaths = JsEnvironmentConfigurator.getRuntimePathsForModule(module, testServices)
    val transitiveLibraries = JsEnvironmentConfigurator.getKlibDependencies(module, testServices, DependencyRelation.RegularDependency)
    val friendLibraries = JsEnvironmentConfigurator.getKlibDependencies(module, testServices, DependencyRelation.FriendDependency)
    return Triple(runtimeKlibsPaths, transitiveLibraries, friendLibraries)
}

private fun getAllJsDependenciesPaths(module: TestModule, testServices: TestServices): List<String> {
    val (runtimeKlibsPaths, transitiveLibraries, friendLibraries) = getJsDependencies(module, testServices)
    return runtimeKlibsPaths + transitiveLibraries.map { it.path } + friendLibraries.map { it.path }
}

fun resolveJsLibraries(
    module: TestModule,
    testServices: TestServices,
    configuration: CompilerConfiguration
): List<KotlinResolvedLibrary> {
    val paths = getAllJsDependenciesPaths(module, testServices)
    val repositories = configuration[JSConfigurationKeys.REPOSITORIES] ?: emptyList()
    val logger = configuration[IrMessageLogger.IR_MESSAGE_LOGGER].toResolverLogger()
    return jsResolveLibraries(paths, repositories, logger).getFullResolvedList()
}

private fun buildDependencyList(
    module: TestModule,
    moduleName: Name,
    moduleInfoProvider: FirModuleInfoProvider,
    analyzerServices: PlatformDependentAnalyzerServices,
    configureDependencies: DependencyListForCliModule.Builder.() -> Unit,
) = DependencyListForCliModule.build(moduleName, module.targetPlatform, analyzerServices) {
    configureDependencies()
    sourceDependencies(moduleInfoProvider.getRegularDependentSourceModules(module))
    sourceFriendsDependencies(moduleInfoProvider.getDependentFriendSourceModules(module))
    sourceDependsOnDependencies(moduleInfoProvider.getDependentDependsOnSourceModules(module))
}

private fun configureSessionsForJvmOrCommon(
    module: TestModule,
    moduleName: Name,
    project: Project,
    ktFiles: Collection<KtFile>,
    compilerConfigurationProvider: CompilerConfigurationProvider,
    moduleInfoProvider: FirModuleInfoProvider,
    analyzerServices: PlatformDependentAnalyzerServices,
    configuration: CompilerConfiguration,
    sessionConfigurator: FirSessionFactory.FirSessionConfigurator.() -> Unit,
    extensionRegistrars: List<FirExtensionRegistrar>,
    languageVersionSettings: LanguageVersionSettings = LanguageVersionSettingsImpl.DEFAULT,
): FirSession {
    val librariesScope = ProjectScope.getLibrariesScope(project)
    val sourcesScope = TopDownAnalyzerFacadeForJVM.newModuleSearchScope(project, ktFiles)

    val dependencyList = buildDependencyList(module, moduleName, moduleInfoProvider, analyzerServices) {
        configureJvmDependencies(configuration)
    }

    configureLibrarySessionForJvmOrCommon(
        module,
        moduleName,
        moduleInfoProvider.firSessionProvider,
        dependencyList,
        librariesScope,
        project,
        compilerConfigurationProvider,
        languageVersionSettings,
    )

    val mainModuleData = FirModuleDataImpl(
        moduleName,
        dependencyList.regularDependencies,
        dependencyList.dependsOnDependencies,
        dependencyList.friendsDependencies,
        dependencyList.platform,
        dependencyList.analyzerServices
    )

    return configureMainSessionForJvmOrCommon(
        module,
        project,
        compilerConfigurationProvider,
        sessionConfigurator,
        extensionRegistrars,
        sourcesScope,
        mainModuleData,
        moduleInfoProvider.firSessionProvider,
        languageVersionSettings,
    )
}

private fun configureLibrarySessionForJvmOrCommon(
    module: TestModule,
    moduleName: Name,
    sessionProvider: FirProjectSessionProvider,
    dependencyList: DependencyListForCliModule,
    librariesScope: GlobalSearchScope,
    project: Project,
    compilerConfigurationProvider: CompilerConfigurationProvider,
    languageVersionSettings: LanguageVersionSettings = LanguageVersionSettingsImpl.DEFAULT,
): FirSession {
    val packagePartProviderFactory = compilerConfigurationProvider.getPackagePartProviderFactory(module)

    val projectEnvironment = VfsBasedProjectEnvironment(
        project, VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL),
    ) { packagePartProviderFactory.invoke(it) }

    val projectFileSearchScope = PsiBasedProjectFileSearchScope(librariesScope)

    return FirSessionFactory.createLibrarySession(
        moduleName,
        sessionProvider,
        dependencyList.moduleDataProvider,
        projectFileSearchScope,
        projectEnvironment,
        projectEnvironment.getPackagePartProvider(projectFileSearchScope),
        languageVersionSettings,
    )
}

private fun configureMainSessionForJvmOrCommon(
    module: TestModule,
    project: Project,
    compilerConfigurationProvider: CompilerConfigurationProvider,
    sessionConfigurator: FirSessionFactory.FirSessionConfigurator.() -> Unit,
    extensionRegistrars: List<FirExtensionRegistrar>,
    sourcesScope: GlobalSearchScope,
    mainModuleData: FirModuleData,
    sessionProvider: FirProjectSessionProvider,
    languageVersionSettings: LanguageVersionSettings = LanguageVersionSettingsImpl.DEFAULT,
): FirSession {
    val packagePartProviderFactory = compilerConfigurationProvider.getPackagePartProviderFactory(module)

    val projectEnvironment = VfsBasedProjectEnvironment(
        project, VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL),
    ) { packagePartProviderFactory.invoke(it) }

    val projectFileSearchScope = PsiBasedProjectFileSearchScope(sourcesScope)

    return FirSessionFactory.createJavaModuleBasedSession(
        mainModuleData,
        sessionProvider,
        projectFileSearchScope,
        projectEnvironment,
        incrementalCompilationContext = null,
        extensionRegistrars,
        languageVersionSettings,
        lookupTracker = null,
        enumWhenTracker = null,
        needRegisterJavaElementFinder = true,
        sessionConfigurator,
    )
}

private fun configureSessionsForJs(
    module: TestModule,
    moduleName: Name,
    moduleInfoProvider: FirModuleInfoProvider,
    analyzerServices: PlatformDependentAnalyzerServices,
    configuration: CompilerConfiguration,
    testServices: TestServices,
    sessionConfigurator: FirSessionFactory.FirSessionConfigurator.() -> Unit,
    extensionRegistrars: List<FirExtensionRegistrar>,
    languageVersionSettings: LanguageVersionSettings = LanguageVersionSettingsImpl.DEFAULT,
): FirSession {
    val dependencyList = buildDependencyList(module, moduleName, moduleInfoProvider, analyzerServices) {
        configureJsDependencies(module, testServices)
    }

    FirJsSessionFactory.createJsLibrarySession(
        moduleName,
        module,
        testServices,
        configuration,
        moduleInfoProvider.firSessionProvider,
        dependencyList.moduleDataProvider,
        languageVersionSettings,
    )

    val mainModuleData = FirModuleDataImpl(
        moduleName,
        dependencyList.regularDependencies,
        dependencyList.dependsOnDependencies,
        dependencyList.friendsDependencies,
        dependencyList.platform,
        dependencyList.analyzerServices
    )

    return FirJsSessionFactory.createJsModuleBasedSession(
        mainModuleData,
        moduleInfoProvider.firSessionProvider,
        extensionRegistrars,
        languageVersionSettings,
        null,
        sessionConfigurator,
    )
}

fun TargetPlatform.getAnalyzerServices(): PlatformDependentAnalyzerServices {
    return when {
        isJvm() -> JvmPlatformAnalyzerServices
        isJs() -> JsPlatformAnalyzerServices
        isNative() -> NativePlatformAnalyzerServices
        isCommon() -> CommonPlatformAnalyzerServices
        else -> error("Unknown target platform: $this")
    }
}
