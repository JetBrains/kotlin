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
import org.jetbrains.kotlin.cli.jvm.compiler.PsiBasedProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.PsiBasedProjectFileSearchScope
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.jvmModularRoots
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.FirAnalyzerFacade
import org.jetbrains.kotlin.fir.checkers.registerExtendedCommonCheckers
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.fir.session.FirSessionFactory
import org.jetbrains.kotlin.ir.backend.js.jsResolveLibraries
import org.jetbrains.kotlin.ir.backend.js.toResolverLogger
import org.jetbrains.kotlin.ir.util.IrMessageLogger
import org.jetbrains.kotlin.js.analyze.AbstractTopDownAnalyzerFacadeForJS
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
        val (ktFiles, originalFiles) = if (lightTreeEnabled) {
            emptyList<KtFile>() to module.files.filter { it.isKtFile }.map { testServices.sourceFileProvider.getRealFileForSourceFile(it) }
        } else {
            testServices.sourceFileProvider.getKtFilesForSourceFiles(module.files, project).values to emptyList()
        }

        val languageVersionSettings = module.languageVersionSettings

        val configuration = compilerConfigurationProvider.getCompilerConfiguration(module)

        val librariesScope = ProjectScope.getLibrariesScope(project)

        val sourcesScope = when {
            module.targetPlatform.isJvm() -> TopDownAnalyzerFacadeForJVM.newModuleSearchScope(project, ktFiles)
            module.targetPlatform.isJs() -> AbstractTopDownAnalyzerFacadeForJS.newModuleSearchScope(project, ktFiles)
            else -> throw Exception("Unsupported")
        }

        val moduleName = Name.identifier(module.name)
        val analyzerServices = module.targetPlatform.getAnalyzerServices()

        val dependencyList = DependencyListForCliModule.build(moduleName, module.targetPlatform, analyzerServices) {
            when {
                module.targetPlatform.isJvm() -> configureJvmDependencies(configuration)
                module.targetPlatform.isJs() -> configureJsDependencies(module, testServices)
                else -> throw Exception("Unsupported")
            }

            sourceDependencies(moduleInfoProvider.getRegularDependentSourceModules(module))
            sourceFriendsDependencies(moduleInfoProvider.getDependentFriendSourceModules(module))
            sourceDependsOnDependencies(moduleInfoProvider.getDependentDependsOnSourceModules(module))
        }

        configureLibrarySession(
            module,
            moduleName,
            testServices,
            configuration,
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

        val session = configureMainSession(
            module,
            mainModuleData,
            moduleInfoProvider.firSessionProvider,
            sourcesScope,
            project,
            compilerConfigurationProvider,
            additionalSessionConfiguration,
            languageVersionSettings,
        )

        moduleInfoProvider.registerModuleData(module, session.moduleData)

        val enablePluginPhases = FirDiagnosticsDirectives.ENABLE_PLUGIN_PHASES in module.directives
        val firAnalyzerFacade = FirAnalyzerFacade(
            session,
            languageVersionSettings,
            ktFiles,
            originalFiles,
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
    val (runtimeKlibsNames, transitiveLibraries, friendLibraries) = getJsDependencies(module, testServices)

    dependencies(runtimeKlibsNames.map { Paths.get(it).toAbsolutePath() })
    dependencies(transitiveLibraries.map { it.toPath().toAbsolutePath() })

    friendDependencies(friendLibraries.map { it.toPath().toAbsolutePath() })
}

private fun getJsDependencies(module: TestModule, testServices: TestServices): Triple<List<String>, List<File>, List<File>> {
    val runtimeKlibsNames = JsEnvironmentConfigurator.getRuntimePathsForModule(module, testServices)
    val transitiveLibraries = JsEnvironmentConfigurator.getKlibDependencies(module, testServices, DependencyRelation.RegularDependency)
    val friendLibraries = JsEnvironmentConfigurator.getKlibDependencies(module, testServices, DependencyRelation.FriendDependency)
    return Triple(runtimeKlibsNames, transitiveLibraries, friendLibraries)
}

private fun getAllJsDependenciesNames(module: TestModule, testServices: TestServices): List<String> {
    val (runtimeKlibsNames, transitiveLibraries, friendLibraries) = getJsDependencies(module, testServices)
    return runtimeKlibsNames + transitiveLibraries.map { it.name } + friendLibraries.map { it.name }
}

fun resolveJsLibraries(
    module: TestModule,
    testServices: TestServices,
    configuration: CompilerConfiguration
): List<KotlinResolvedLibrary> {
    val names = getAllJsDependenciesNames(module, testServices)
    val repositories = configuration[JSConfigurationKeys.REPOSITORIES] ?: emptyList()
    val logger = configuration[IrMessageLogger.IR_MESSAGE_LOGGER].toResolverLogger()
    return jsResolveLibraries(names, repositories, logger).getFullResolvedList()
}

@OptIn(PrivateSessionConstructor::class, SessionConfiguration::class)
private fun configureLibrarySession(
    module: TestModule,
    moduleName: Name,
    testServices: TestServices,
    configuration: CompilerConfiguration,
    sessionProvider: FirProjectSessionProvider,
    dependencyList: DependencyListForCliModule,
    librariesScope: GlobalSearchScope,
    project: Project,
    compilerConfigurationProvider: CompilerConfigurationProvider,
    languageVersionSettings: LanguageVersionSettings = LanguageVersionSettingsImpl.DEFAULT,
): FirSession = when {
    module.targetPlatform.isJvm() -> {
        val packagePartProviderFactory = compilerConfigurationProvider.getPackagePartProviderFactory(module)
        val localFileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL)

        val projectEnvironment = PsiBasedProjectEnvironment(project, localFileSystem) {
            packagePartProviderFactory.invoke(it)
        }

        val projectFileSearchScope = PsiBasedProjectFileSearchScope(librariesScope)

        FirSessionFactory.createLibrarySession(
            moduleName,
            sessionProvider,
            dependencyList.moduleDataProvider,
            projectFileSearchScope,
            projectEnvironment,
            projectEnvironment.getPackagePartProvider(projectFileSearchScope),
            languageVersionSettings,
        )
    }
    module.targetPlatform.isJs() -> {
        FirJsSessionFactory.createJsLibrarySession(
            moduleName,
            module,
            testServices,
            configuration,
            sessionProvider,
            dependencyList.moduleDataProvider,
            languageVersionSettings,
        )
    }
    else -> throw Exception("Unsupported")
}

@OptIn(PrivateSessionConstructor::class, SessionConfiguration::class)
private fun configureMainSession(
    module: TestModule,
    mainModuleData: FirModuleData,
    sessionProvider: FirProjectSessionProvider,
    sourcesScope: GlobalSearchScope,
    project: Project,
    compilerConfigurationProvider: CompilerConfigurationProvider,
    additionalSessionConfiguration: FirFrontendFacade.SessionConfiguration?,
    languageVersionSettings: LanguageVersionSettings = LanguageVersionSettingsImpl.DEFAULT,
): FirSession {
    val sessionConfigurator: FirSessionFactory.FirSessionConfigurator.() -> Unit = {
        if (FirDiagnosticsDirectives.WITH_EXTENDED_CHECKERS in module.directives) {
            registerExtendedCommonCheckers()
        }
        additionalSessionConfiguration?.invoke(this)
    }

    val extensionRegistrars = FirExtensionRegistrar.getInstances(project)

    return when {
        module.targetPlatform.isJvm() -> {
            val packagePartProviderFactory = compilerConfigurationProvider.getPackagePartProviderFactory(module)
            val localFileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL)

            val projectEnvironment = PsiBasedProjectEnvironment(project, localFileSystem) {
                packagePartProviderFactory.invoke(it)
            }

            val projectFileSearchScope = PsiBasedProjectFileSearchScope(sourcesScope)

            FirSessionFactory.createJavaModuleBasedSession(
                mainModuleData,
                sessionProvider,
                projectFileSearchScope,
                projectEnvironment,
                null,
                extensionRegistrars,
                languageVersionSettings,
                null,
                sessionConfigurator,
            )
        }
        module.targetPlatform.isJs() -> {
            FirJsSessionFactory.createJsModuleBasedSession(
                mainModuleData,
                sessionProvider,
                extensionRegistrars,
                languageVersionSettings,
                null,
                sessionConfigurator,
            )
        }
        else -> throw Exception("Unsupported")
    }
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
