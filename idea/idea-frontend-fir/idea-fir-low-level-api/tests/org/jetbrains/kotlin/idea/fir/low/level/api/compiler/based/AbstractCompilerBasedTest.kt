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
import org.jetbrains.kotlin.idea.fir.low.level.api.api.*
import org.jetbrains.kotlin.idea.fir.low.level.api.test.base.DeclarationProviderTestImpl
import org.jetbrains.kotlin.idea.fir.low.level.api.test.base.KotlinOutOfBlockModificationTrackerFactoryTestImpl
import org.jetbrains.kotlin.idea.fir.low.level.api.test.base.KtPackageProviderTestImpl
import org.jetbrains.kotlin.idea.fir.low.level.api.test.base.SealedClassInheritorsProviderTestImpl
import org.jetbrains.kotlin.idea.fir.low.level.api.transformers.FirLazyTransformerForIDE
import org.jetbrains.kotlin.idea.frontend.api.InvalidWayOfUsingAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSessionProvider
import org.jetbrains.kotlin.idea.frontend.api.fir.KtFirAnalysisSessionProvider
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.PlatformDependentAnalyzerServices
import org.jetbrains.kotlin.test.TestConfiguration
import org.jetbrains.kotlin.test.bind
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.testConfiguration
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
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

        useAdditionalService(::TestModuleInfoProvider)
        usePreAnalysisHandlers(::ModuleRegistrarPreAnalysisHandler.bind(disposable))
    }

    open fun TestConfigurationBuilder.configureTest() {}


    inner class LowLevelFirFrontendFacade(
        testServices: TestServices
    ) : FrontendFacade<FirOutputArtifact>(testServices, FrontendKinds.FIR) {

        override val directiveContainers: List<DirectivesContainer>
            get() = listOf(FirDiagnosticsDirectives)

        override fun analyze(module: TestModule): FirOutputArtifact {
            val moduleInfoProvider = testServices.moduleInfoProvider
            val moduleInfo = moduleInfoProvider.getModuleInfo(module.name)

            val project = testServices.compilerConfigurationProvider.getProject(module)
            val resolveState = createResolveStateForNoCaching(moduleInfo, project)

            val allFirFiles = moduleInfo.testFilesToKtFiles.map { (testFile, psiFile) ->
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

        if (modules.modules.none { it.files.any { it.isKtFile } }) {
            return true // nothing to highlight
        }

        return false
    }
}


class TestModuleInfoProvider(private val testServices: TestServices) : TestService {
    private val cache = mutableMapOf<String, TestModuleSourceInfo>()

    fun registerModuleInfo(testModule: TestModule, ktFiles: Map<TestFile, KtFile>) {
        cache[testModule.name] = TestModuleSourceInfo(testModule, ktFiles, testServices)
    }

    fun getModuleInfoByKtFile(ktFile: KtFile): TestModuleSourceInfo =
        cache.values.first { moduleSourceInfo ->
            ktFile in moduleSourceInfo.ktFiles
        }

    fun getModuleInfo(moduleName: String): TestModuleSourceInfo =
        cache.getValue(moduleName)
}

val TestServices.moduleInfoProvider: TestModuleInfoProvider by TestServices.testServiceAccessor()

class ModuleRegistrarPreAnalysisHandler(
    testServices: TestServices,
    private val parentDisposable: Disposable
) : PreAnalysisHandler(testServices) {

    private val moduleInfoProvider = testServices.moduleInfoProvider

    override fun preprocessModuleStructure(moduleStructure: TestModuleStructure) {
        // todo rework after all modules will have the same Project instance
        val ktFilesByModule = moduleStructure.modules.associateWith { testModule ->
            val project = testServices.compilerConfigurationProvider.getProject(testModule)
            testServices.sourceFileProvider.getKtFilesForSourceFiles(testModule.files, project)
        }

        val allKtFiles = ktFilesByModule.values.flatMap { it.values.toList() }

        ktFilesByModule.forEach { (testModule, ktFiles) ->
            val project = testServices.compilerConfigurationProvider.getProject(testModule)
            moduleInfoProvider.registerModuleInfo(testModule, ktFiles)

            val configurator = FirModuleResolveStateConfiguratorForSingleModuleTestImpl(
                project,
                testServices,
                parentDisposable
            )
            (project as MockProject).registerTestServices(configurator, allKtFiles)
        }
    }
}

class TestModuleSourceInfo(
    val testModule: TestModule,
    val testFilesToKtFiles: Map<TestFile, KtFile>,
    testServices: TestServices,
) : ModuleInfo, ModuleSourceInfoBase {
    private val moduleInfoProvider = testServices.moduleInfoProvider

    val ktFiles = testFilesToKtFiles.values.toSet()
    val moduleData = FirModuleInfoBasedModuleData(this)

    override val name: Name
        get() = Name.identifier(testModule.name)

    override fun dependencies(): List<ModuleInfo> =
        testModule.allDependencies
            .map { moduleInfoProvider.getModuleInfo(it.moduleName) }

    override val expectedBy: List<ModuleInfo>
        get() = testModule.dependsOnDependencies
            .map { moduleInfoProvider.getModuleInfo(it.moduleName) }

    override fun modulesWhoseInternalsAreVisible(): Collection<ModuleInfo> =
        testModule.friendDependencies
            .map { moduleInfoProvider.getModuleInfo(it.moduleName) }

    override val platform: TargetPlatform
        get() = testModule.targetPlatform

    override val analyzerServices: PlatformDependentAnalyzerServices
        get() = testModule.targetPlatform.getAnalyzerServices()
}


class FirModuleResolveStateConfiguratorForSingleModuleTestImpl(
    private val project: Project,
    testServices: TestServices,
    private val parentDisposable: Disposable,
) : FirModuleResolveStateConfigurator() {
    private val moduleInfoProvider = testServices.moduleInfoProvider
    private val compilerConfigurationProvider = testServices.compilerConfigurationProvider
    private val librariesScope = ProjectScope.getLibrariesScope(project)//todo incorrect?
    private val sealedClassInheritorsProvider = SealedClassInheritorsProviderTestImpl()

    override fun createPackagePartsProvider(moduleInfo: ModuleSourceInfoBase, scope: GlobalSearchScope): PackagePartProvider {
        require(moduleInfo is TestModuleSourceInfo)
        val factory = compilerConfigurationProvider.getPackagePartProviderFactory(moduleInfo.testModule)
        return factory(scope)
    }


    override fun createModuleDataProvider(moduleInfo: ModuleSourceInfoBase): ModuleDataProvider {
        require(moduleInfo is TestModuleSourceInfo)
        val configuration = compilerConfigurationProvider.getCompilerConfiguration(moduleInfo.testModule)
        return DependencyListForCliModule.build(
            moduleInfo.name,
            moduleInfo.platform,
            moduleInfo.analyzerServices
        ) {
            dependencies(configuration.jvmModularRoots.map { it.toPath() })
            dependencies(configuration.jvmClasspathRoots.map { it.toPath() })

            friendDependencies(configuration[JVMConfigurationKeys.FRIEND_PATHS] ?: emptyList())

            sourceDependencies(moduleInfo.moduleData.dependencies)
            sourceFriendsDependencies(moduleInfo.moduleData.friendDependencies)
            sourceDependsOnDependencies(moduleInfo.moduleData.dependsOnDependencies)
        }.moduleDataProvider
    }

    override fun getLanguageVersionSettings(moduleInfo: ModuleSourceInfoBase): LanguageVersionSettings {
        require(moduleInfo is TestModuleSourceInfo)
        return moduleInfo.testModule.languageVersionSettings
    }

    override fun getModuleSourceScope(moduleInfo: ModuleSourceInfoBase): GlobalSearchScope {
        require(moduleInfo is TestModuleSourceInfo)
        return TopDownAnalyzerFacadeForJVM.newModuleSearchScope(project, moduleInfo.testFilesToKtFiles.values)
    }

    override fun createScopeForModuleLibraries(moduleInfo: ModuleSourceInfoBase): GlobalSearchScope {
        return librariesScope
    }

    override fun createSealedInheritorsProvider(): SealedClassInheritorsProvider {
        return sealedClassInheritorsProvider
    }

    override fun getModuleInfoFor(element: KtElement): ModuleInfo {
        val containingFile = element.containingKtFile
        return moduleInfoProvider.getModuleInfoByKtFile(containingFile)
    }

    override fun configureSourceSession(session: FirSession) {
    }
}

fun MockProject.registerTestServices(
    configurator: FirModuleResolveStateConfiguratorForSingleModuleTestImpl,
    allKtFiles: List<KtFile>,
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
            return DeclarationProviderTestImpl(searchScope, allKtFiles.filter { searchScope.contains(it.virtualFile) })
        }
    })
    registerService(KtPackageProviderFactory::class.java, object : KtPackageProviderFactory() {
        override fun createPackageProvider(searchScope: GlobalSearchScope): KtPackageProvider {
            return KtPackageProviderTestImpl(searchScope, allKtFiles.filter { searchScope.contains(it.virtualFile) })
        }
    })
    reRegisterJavaElementFinder(this)
}

@OptIn(InvalidWayOfUsingAnalysisSession::class)
private fun reRegisterJavaElementFinder(project: Project) {
    PsiElementFinder.EP.getPoint(project).unregisterExtension(JavaElementFinder::class.java)
    with(project as MockProject) {
        picoContainer.registerComponentInstance(
            KtAnalysisSessionProvider::class.qualifiedName,
            KtFirAnalysisSessionProvider(this)
        )
        picoContainer.unregisterComponent(KotlinAsJavaSupport::class.qualifiedName)
        picoContainer.registerComponentInstance(
            KotlinAsJavaSupport::class.qualifiedName,
            IDEKotlinAsJavaFirSupport(project)
        )
    }
    @Suppress("DEPRECATION")
    PsiElementFinder.EP.getPoint(project).registerExtension(JavaElementFinder(project))
}
