/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir

import com.intellij.openapi.vfs.StandardFileSystems.FILE_PROTOCOL
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.search.ProjectScope.getLibrariesScope
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder
import org.jetbrains.kotlin.cli.jvm.compiler.*
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.checkers.registerExperimentalCheckers
import org.jetbrains.kotlin.fir.checkers.registerExtraCommonCheckers
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.session.FirJvmSessionFactory
import org.jetbrains.kotlin.fir.session.FirSharableJavaComponents
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectEnvironment
import org.jetbrains.kotlin.fir.session.firCachesFactoryForCliMode
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.singleValue
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDiagnosticCollectorService
import org.jetbrains.kotlin.test.model.FrontendFacade
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*

open class FirReplFrontendFacade(testServices: TestServices) : FrontendFacade<FirOutputArtifact>(testServices, FrontendKinds.FIR) {

    override val additionalServices: List<ServiceRegistrationData>
        get() = listOf(
            service(::FirModuleInfoProvider),
            service(::FirDiagnosticCollectorService),
        )

    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(FirDiagnosticsDirectives)

    private fun registerExtraComponents(session: FirSession) {
        testServices.firSessionComponentRegistrar?.registerAdditionalComponent(session)
    }

    private class ReplCompilationEnvironment(
        val targetPlatform: TargetPlatform,
        val extensionRegistrars: List<FirExtensionRegistrar>,
        val predefinedJavaComponents: FirSharableJavaComponents?,
        val projectEnvironment: AbstractProjectEnvironment,
        val libraryList: DependencyListForCliModule
    )

    @OptIn(SessionConfiguration::class)
    private val replCompilationEnvironment: ReplCompilationEnvironment by lazy {
        val testModule = testServices.moduleStructure.modules.first()
        val targetPlatform = testModule.targetPlatform(testServices)

        require(targetPlatform.isJvm())

        val compilerConfigurationProvider = testServices.compilerConfigurationProvider

        val project = testServices.compilerConfigurationProvider.getProject(testModule)
        val configuration = compilerConfigurationProvider.getCompilerConfiguration(testModule)
        val libraryList = createLibraryListForJvm("repl", configuration, emptyList())
        val extensionRegistrars = FirExtensionRegistrar.getInstances(project)
        val predefinedJavaComponents = FirSharableJavaComponents(firCachesFactoryForCliMode)
        val packagePartProviderFactory = compilerConfigurationProvider.getPackagePartProviderFactory(testModule)
        val librariesSearchScope = PsiBasedProjectFileSearchScope(getLibrariesScope(project))

        val projectEnvironment =
            VfsBasedProjectEnvironment(project, VirtualFileManager.getInstance().getFileSystem(FILE_PROTOCOL)) {
                packagePartProviderFactory.invoke(it)
            }

        val sharedLibrarySession = FirJvmSessionFactory.createSharedLibrarySession(
            Name.special("<${testModule.name}>"),
            testServices.firModuleInfoProvider.firSessionProvider,
            projectEnvironment,
            extensionRegistrars,
            librariesSearchScope,
            projectEnvironment.getPackagePartProvider(librariesSearchScope),
            testModule.languageVersionSettings,
            predefinedJavaComponents,
        )

        FirJvmSessionFactory.createLibrarySession(
            testServices.firModuleInfoProvider.firSessionProvider,
            sharedLibrarySession,
            libraryList.moduleDataProvider,
            projectEnvironment,
            extensionRegistrars,
            librariesSearchScope,
            projectEnvironment.getPackagePartProvider(librariesSearchScope),
            testModule.languageVersionSettings,
            predefinedJavaComponents,
        ).also(::registerExtraComponents)

        ReplCompilationEnvironment(
            targetPlatform,
            extensionRegistrars,
            predefinedJavaComponents,
            projectEnvironment,
            libraryList
        )
    }

    override fun analyze(module: TestModule): FirOutputArtifact {
        val moduleData = initializeModuleData(module)

        val firOutputPart = analyzeImpl(module, moduleData)

        return FirOutputArtifactImpl(listOf(firOutputPart))
    }

    private fun initializeModuleData(module: TestModule): FirModuleData {
        val moduleInfoProvider = testServices.firModuleInfoProvider
        val libraryList = replCompilationEnvironment.libraryList

        val regularModules = libraryList.regularDependencies + moduleInfoProvider.getRegularDependentSourceModules(module)
        // TODO: collect instead of recursive traversal on each new snippet
        val friendModules = libraryList.friendDependencies + moduleInfoProvider.getDependentFriendSourceModulesRecursively(module)
        val dependsOnModules = libraryList.dependsOnDependencies + moduleInfoProvider.getDependentDependsOnSourceModules(module)

        return FirSourceModuleData(
            Name.special("<${module.name}>"),
            regularModules,
            dependsOnModules,
            friendModules,
            replCompilationEnvironment.targetPlatform,
            isCommon = false,
        ).also {
            moduleInfoProvider.registerModuleData(module, it)
        }
    }

    private fun analyzeImpl(module: TestModule, moduleData: FirModuleData): FirOutputPartForDependsOnModule {
        val firParser = module.directives.singleValue(FirDiagnosticsDirectives.FIR_PARSER)

        require(firParser == FirParser.Psi)

        val compilerConfigurationProvider = testServices.compilerConfigurationProvider
        val compilerConfiguration = compilerConfigurationProvider.getCompilerConfiguration(module)
        val project = compilerConfigurationProvider.getProject(module)

        PsiElementFinder.EP.getPoint(project).unregisterFinders<JavaElementFinder>()

        val ktFiles = testServices.sourceFileProvider.getKtFilesForSourceFiles(module.files, project)

        val moduleBasedSession = FirJvmSessionFactory.createSourceSession(
            moduleData = moduleData,
            sessionProvider = testServices.firModuleInfoProvider.firSessionProvider,
            javaSourcesScope = PsiBasedProjectFileSearchScope(TopDownAnalyzerFacadeForJVM.newModuleSearchScope(project, ktFiles.values)),
            projectEnvironment = replCompilationEnvironment.projectEnvironment,
            createIncrementalCompilationSymbolProviders = { null },
            extensionRegistrars = replCompilationEnvironment.extensionRegistrars,
            configuration = compilerConfiguration,
            predefinedJavaComponents = replCompilationEnvironment.predefinedJavaComponents,
            needRegisterJavaElementFinder = true,
        ) {
            if (FirDiagnosticsDirectives.WITH_EXTRA_CHECKERS in module.directives) {
                registerExtraCommonCheckers()
            }
            if (FirDiagnosticsDirectives.WITH_EXPERIMENTAL_CHECKERS in module.directives) {
                registerExperimentalCheckers()
            }
        }.also(::registerExtraComponents)

        val firAnalyzerFacade = FirAnalyzerFacade(moduleBasedSession, ktFiles.values, parser = firParser)
        val firFiles = firAnalyzerFacade.runResolution()

        val filesMap = ktFiles.keys
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
}
