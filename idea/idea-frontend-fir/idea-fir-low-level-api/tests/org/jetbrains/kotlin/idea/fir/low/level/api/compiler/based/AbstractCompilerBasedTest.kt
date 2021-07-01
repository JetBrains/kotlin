/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.compiler.based

import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.analyzer.ModuleSourceInfoBase
import org.jetbrains.kotlin.asJava.KotlinAsJavaSupport
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.jvmModularRoots
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.fir.DependencyListForCliModule
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.SealedClassInheritorsProvider
import org.jetbrains.kotlin.fir.deserialization.ModuleDataProvider
import org.jetbrains.kotlin.fir.session.FirModuleInfoBasedModuleData
import org.jetbrains.kotlin.idea.asJava.IDEKotlinAsJavaFirSupport
import org.jetbrains.kotlin.idea.fir.low.level.api.*
import org.jetbrains.kotlin.idea.fir.low.level.api.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirModuleResolveStateConfigurator
import org.jetbrains.kotlin.idea.fir.low.level.api.api.KotlinOutOfBlockModificationTrackerFactory
import org.jetbrains.kotlin.idea.fir.low.level.api.api.getOrBuildFirFile
import org.jetbrains.kotlin.idea.fir.low.level.api.test.base.*
import org.jetbrains.kotlin.idea.fir.low.level.api.test.base.AbstractLowLevelApiTest.Companion.reRegisterJavaElementFinder
import org.jetbrains.kotlin.idea.fir.low.level.api.test.base.DeclarationProviderTestImpl
import org.jetbrains.kotlin.idea.fir.low.level.api.test.base.KotlinOutOfBlockModificationTrackerFactoryTestImpl
import org.jetbrains.kotlin.idea.fir.low.level.api.test.base.KtPackageProviderTestImpl
import org.jetbrains.kotlin.idea.fir.low.level.api.test.base.SealedClassInheritorsProviderTestImpl
import org.jetbrains.kotlin.idea.fir.low.level.api.transformers.FirLazyTransformerForIDE
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.PlatformDependentAnalyzerServices
import org.jetbrains.kotlin.test.TestConfiguration
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.testConfiguration
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.frontend.fir.FirModuleInfoProvider
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.firModuleInfoProvider
import org.jetbrains.kotlin.test.frontend.fir.getAnalyzerServices
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerTest
import org.jetbrains.kotlin.test.services.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo

abstract class AbstractCompilerBasedTest : AbstractKotlinCompilerTest() {
    private var _disposable: Disposable? = null
    protected val disposable: Disposable get() = _disposable!!

    @BeforeEach
    private fun intiDisposable(testInfo: TestInfo) {
        _disposable = Disposer.newDisposable("disposable for ${testInfo.displayName}")
    }

    @AfterEach
    private fun disposeDisposable() {
        _disposable?.let { Disposer.dispose(it) }
        _disposable = null
    }

    final override fun TestConfigurationBuilder.configuration() {
        globalDefaults {
            frontend = FrontendKinds.FIR
            targetPlatform = JvmPlatforms.defaultJvmPlatform
            dependencyKind = DependencyKind.Source
        }

        configureTest()
        defaultConfiguration(this)
        unregisterAllFacades()
        useFrontendFacades(::LowLevelFirFrontendFacade)
    }

    open fun TestConfigurationBuilder.configureTest() {}


    inner class LowLevelFirFrontendFacade(
        testServices: TestServices
    ) : FrontendFacade<FirOutputArtifact>(testServices, FrontendKinds.FIR) {
        override val additionalServices: List<ServiceRegistrationData>
            get() = listOf(service(::FirModuleInfoProvider))

        override val additionalDirectives: List<DirectivesContainer>
            get() = listOf(FirDiagnosticsDirectives)

        override fun analyze(module: TestModule): FirOutputArtifact {
            val project = testServices.compilerConfigurationProvider.getProject(module)
            val ktFiles = testServices.sourceFileProvider.getKtFilesForSourceFiles(module.files, project)

            reRegisterJavaElementFinder(project)

            val moduleInfo = TestModuleInfo(module)
            testServices.firModuleInfoProvider.registerModuleData(module, FirModuleInfoBasedModuleData(moduleInfo))
            val configurator =
                FirModuleResolveStateConfiguratorForSingleModuleTestImpl(testServices, module, ktFiles, moduleInfo, disposable)

            with(project as MockProject) {
                registerTestServices(configurator, ktFiles)
            }

            val resolveState = createResolveStateForNoCaching(moduleInfo, project)

            val allFirFiles = ktFiles.map { (testFile, psiFile) ->
                testFile to psiFile.getOrBuildFirFile(resolveState)
            }.toMap()

            val diagnosticCheckerFilter = if (FirDiagnosticsDirectives.WITH_EXTENDED_CHECKERS in module.directives) {
                DiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS
            } else DiagnosticCheckerFilter.ONLY_COMMON_CHECKERS

            val analyzerFacade = LowLevelFirAnalyzerFacade(resolveState, allFirFiles, diagnosticCheckerFilter)
            return LowLevelFirOutputArtifact(resolveState.rootModuleSession, analyzerFacade)
        }
    }

    override fun runTest(filePath: String) {
        val configuration = testConfiguration(filePath, configuration)
        if (ignoreTest(filePath, configuration)) {
            return
        }
        val oldEnableDeepEnsure = FirLazyTransformerForIDE.enableDeepEnsure
        try {
            FirLazyTransformerForIDE.enableDeepEnsure = true
            super.runTest(filePath)
        } finally {
            FirLazyTransformerForIDE.enableDeepEnsure = oldEnableDeepEnsure
        }
    }

    private fun ignoreTest(filePath: String, configuration: TestConfiguration): Boolean {
        val modules = configuration.moduleStructureExtractor.splitTestDataByModules(filePath, configuration.directives)

        if (modules.modules.size > 1) {
            return true // multimodule tests are not supported
        }

        val singleModule = modules.modules.single()
        if (singleModule.files.none { it.isKtFile }) {
            return true // nothing to highlight
        }

        return false
    }
}

class TestModuleInfo(
    val testModule: TestModule,
) : ModuleInfo, ModuleSourceInfoBase {
    override val name: Name get() = Name.identifier(testModule.name)

    override fun dependencies(): List<ModuleInfo> {
        return emptyList()
    }

    override val platform: TargetPlatform
        get() = testModule.targetPlatform

    override val analyzerServices: PlatformDependentAnalyzerServices
        get() = testModule.targetPlatform.getAnalyzerServices()
}


class FirModuleResolveStateConfiguratorForSingleModuleTestImpl(
    private val testServices: TestServices,
    private val testModule: TestModule,
    private val ktFiles: Map<TestFile, KtFile>,
    private val moduleInfo: ModuleInfo,
    private val parentDisposable: Disposable,
) : FirModuleResolveStateConfigurator() {
    val moduleInfoProvider = testServices.firModuleInfoProvider
    val compilerConfigurationProvider = testServices.compilerConfigurationProvider

    val project = compilerConfigurationProvider.getProject(testModule)

    val languageVersionSettings = testModule.languageVersionSettings
    val packagePartProviderFactory = compilerConfigurationProvider.getPackagePartProviderFactory(testModule)

    val configuration = compilerConfigurationProvider.getCompilerConfiguration(testModule)

    val librariesScope = ProjectScope.getLibrariesScope(project)
    val sourcesScope = TopDownAnalyzerFacadeForJVM.newModuleSearchScope(project, ktFiles.values)

    private val sealedClassInheritorsProvider = SealedClassInheritorsProviderTestImpl()

    override fun createPackagePartsProvider(scope: GlobalSearchScope): PackagePartProvider {
        return packagePartProviderFactory.invoke(scope)
    }

    override fun createModuleDataProvider(moduleInfo: ModuleSourceInfoBase): ModuleDataProvider {
        return DependencyListForCliModule.build(
            moduleInfo.name,
            moduleInfo.platform,
            moduleInfo.analyzerServices
        ) {
            dependencies(configuration.jvmModularRoots.map { it.toPath() })
            dependencies(configuration.jvmClasspathRoots.map { it.toPath() })

            friendDependencies(configuration[JVMConfigurationKeys.FRIEND_PATHS] ?: emptyList())

            sourceDependencies(moduleInfoProvider.getRegularDependentSourceModules(testModule))
            sourceFriendsDependencies(moduleInfoProvider.getDependentFriendSourceModules(testModule))
            sourceDependsOnDependencies(moduleInfoProvider.getDependentDependsOnSourceModules(testModule))
        }.moduleDataProvider
    }

    override fun getLanguageVersionSettings(moduleInfo: ModuleSourceInfoBase): LanguageVersionSettings {
        return languageVersionSettings
    }

    override fun getModuleSourceScope(moduleInfo: ModuleSourceInfoBase): GlobalSearchScope {
        return sourcesScope
    }

    override fun createScopeForModuleLibraries(moduleInfo: ModuleSourceInfoBase): GlobalSearchScope {
        return librariesScope
    }

    override fun createSealedInheritorsProvider(): SealedClassInheritorsProvider {
        return sealedClassInheritorsProvider
    }

    override fun getModuleInfoFor(element: KtElement): ModuleInfo {
        return moduleInfo
    }

    override fun configureSourceSession(session: FirSession) {
    }
}

fun MockProject.registerTestServices(
    configurator: FirModuleResolveStateConfiguratorForSingleModuleTestImpl,
    ktFiles: Map<TestFile, KtFile>
) {
    registerService(
        FirModuleResolveStateConfigurator::class.java,
        configurator
    )
    registerService(FirIdeResolveStateService::class.java)
    registerService(
        KotlinOutOfBlockModificationTrackerFactory::class.java,
        KotlinOutOfBlockModificationTrackerFactoryTestImpl::class.java
    )
    registerService(KtDeclarationProviderFactory::class.java, object : KtDeclarationProviderFactory() {
        override fun createDeclarationProvider(searchScope: GlobalSearchScope): DeclarationProvider {
            return DeclarationProviderTestImpl(searchScope, ktFiles.values)
        }
    })
    registerService(KtPackageProviderFactory::class.java, object : KtPackageProviderFactory() {
        override fun createPackageProvider(searchScope: GlobalSearchScope): KtPackageProvider {
            return KtPackageProviderTestImpl(searchScope, ktFiles.values)
        }
    })
}
