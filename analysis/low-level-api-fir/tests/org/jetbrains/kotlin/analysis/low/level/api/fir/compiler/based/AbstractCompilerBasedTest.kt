/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.compiler.based

import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import org.jetbrains.kotlin.analysis.api.InvalidWayOfUsingAnalysisSession
import org.jetbrains.kotlin.analysis.api.KtAnalysisSessionProvider
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSessionProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.FirIdeResolveStateService
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirSealedClassInheritorsProcessorFactory
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.PackagePartProviderFactory
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.createResolveStateForNoCaching
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.base.SealedClassInheritorsProviderTestImpl
import org.jetbrains.kotlin.analysis.low.level.api.fir.transformers.FirLazyTransformerForIDE
import org.jetbrains.kotlin.analysis.project.structure.*
import org.jetbrains.kotlin.analysis.providers.KotlinDeclarationProviderFactory
import org.jetbrains.kotlin.analysis.providers.KotlinModificationTrackerFactory
import org.jetbrains.kotlin.analysis.providers.KotlinPackageProviderFactory
import org.jetbrains.kotlin.analysis.providers.impl.KotlinStaticDeclarationProviderFactory
import org.jetbrains.kotlin.analysis.providers.impl.KotlinStaticModificationTrackerFactory
import org.jetbrains.kotlin.analysis.providers.impl.KotlinStaticPackageProviderFactory
import org.jetbrains.kotlin.asJava.KotlinAsJavaSupport
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.jvmModularRoots
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.fir.declarations.SealedClassInheritorsProvider
import org.jetbrains.kotlin.light.classes.symbol.IDEKotlinAsJavaFirSupport
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
import org.jetbrains.kotlin.utils.addIfNotNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

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

        useAdditionalService(::TestKtModuleProvider)
        usePreAnalysisHandlers(::ModuleRegistrarPreAnalysisHandler.bind(disposable))
    }

    open fun TestConfigurationBuilder.configureTest() {}


    inner class LowLevelFirFrontendFacade(
        testServices: TestServices
    ) : FrontendFacade<FirOutputArtifact>(testServices, FrontendKinds.FIR) {

        override val directiveContainers: List<DirectivesContainer>
            get() = listOf(FirDiagnosticsDirectives)

        override fun analyze(module: TestModule): FirOutputArtifact {
            val moduleInfoProvider = testServices.projectModuleProvider
            val moduleInfo = moduleInfoProvider.getModule(module.name)

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


class TestProjectModuleProvider(
    private val testServices: TestServices
) : TestService {
    private val cache = mutableMapOf<String, TestSourceProjectModule>()

    fun registerModuleInfo(project: Project, testModule: TestModule, ktFiles: Map<TestFile, KtFile>) {
        cache[testModule.name] = TestSourceProjectModule(project, testModule, ktFiles, testServices)
    }

    fun getModuleInfoByKtFile(ktFile: KtFile): TestSourceProjectModule =
        cache.values.first { moduleSourceInfo ->
            ktFile in moduleSourceInfo.ktFiles
        }

    fun getModule(moduleName: String): TestSourceProjectModule =
        cache.getValue(moduleName)
}

val TestServices.projectModuleProvider: TestProjectModuleProvider by TestServices.testServiceAccessor()

class ModuleRegistrarPreAnalysisHandler(
    testServices: TestServices,
    private val parentDisposable: Disposable
) : PreAnalysisHandler(testServices) {

    private val moduleInfoProvider = testServices.projectModuleProvider

    override fun preprocessModuleStructure(moduleStructure: TestModuleStructure) {
        // todo rework after all modules will have the same Project instance
        val ktFilesByModule = moduleStructure.modules.associateWith { testModule ->
            val project = testServices.compilerConfigurationProvider.getProject(testModule)
            testServices.sourceFileProvider.getKtFilesForSourceFiles(testModule.files, project)
        }

        val allKtFiles = ktFilesByModule.values.flatMap { it.values.toList() }

        ktFilesByModule.forEach { (testModule, ktFiles) ->
            val project = testServices.compilerConfigurationProvider.getProject(testModule)
            moduleInfoProvider.registerModuleInfo(project, testModule, ktFiles)
            (project as MockProject).registerTestServices(testModule, allKtFiles, testServices)
        }
    }
}

@OptIn(ExperimentalStdlibApi::class)
class TestSourceProjectModule(
    private val project: Project,
    val testModule: TestModule,
    val testFilesToKtFiles: Map<TestFile, KtFile>,
    testServices: TestServices,
) : SourceProjectModule {
    private val moduleProvider = testServices.projectModuleProvider
    private val compilerConfigurationProvider = testServices.compilerConfigurationProvider
    private val configuration = compilerConfigurationProvider.getCompilerConfiguration(testModule)


    val ktFiles = testFilesToKtFiles.values.toSet()

    override val name: Name get() = Name.identifier(testModule.name)

    override val regularDependencies: List<ProjectModule> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        buildList {
            testModule.allDependencies.mapTo(this) { moduleProvider.getModule(it.moduleName) }
            addIfNotNull(
                libraryByRoots(
                    (configuration.jvmModularRoots + configuration.jvmClasspathRoots).map(File::toPath)
                )
            )
        }
    }
    override val dependsOnDependencies: List<ProjectModule> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        testModule.dependsOnDependencies
            .map { moduleProvider.getModule(it.moduleName) }
    }
    override val friendDependencies: List<ProjectModule> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        buildList {
            testModule.friendDependencies.mapTo(this) { moduleProvider.getModule(it.moduleName) }
            addIfNotNull(
                libraryByRoots(configuration[JVMConfigurationKeys.FRIEND_PATHS].orEmpty().map(Paths::get))
            )
        }
    }

    private fun libraryByRoots(roots: List<Path>): LibraryByRoots? {
        if (roots.isEmpty()) return null
        return LibraryByRoots(
            roots,
            this@TestSourceProjectModule,
            project
        )
    }

    override val searchScope: GlobalSearchScope =
        TopDownAnalyzerFacadeForJVM.newModuleSearchScope(project, testFilesToKtFiles.values)

    override val languageVersionSettings: LanguageVersionSettings
        get() = testModule.languageVersionSettings

    override val platform: TargetPlatform
        get() = testModule.targetPlatform

    override val analyzerServices: PlatformDependentAnalyzerServices
        get() = testModule.targetPlatform.getAnalyzerServices()
}

private class LibraryByRoots(
    private val roots: List<Path>,
    private val sourceModule: SourceProjectModule,
    private val project: Project,
) : LibraryProjectModule {
    override val name: Name get() = Name.special("<Libraries>")
    override val regularDependencies: List<ProjectModule> get() = emptyList()
    override val dependsOnDependencies: List<ProjectModule> get() = emptyList()
    override val friendDependencies: List<ProjectModule> get() = emptyList()
    override val searchScope: GlobalSearchScope get() = ProjectScope.getLibrariesScope(project)
    override val platform: TargetPlatform get() = sourceModule.platform
    override val analyzerServices: PlatformDependentAnalyzerServices get() = sourceModule.analyzerServices
    override fun getBinaryRoots(): Collection<Path> = roots
}

fun MockProject.registerTestServices(
    testModule: TestModule,
    allKtFiles: List<KtFile>,
    testServices: TestServices,
) {
    registerService(
        PackagePartProviderFactory::class.java,
        object : PackagePartProviderFactory() {
            override fun createPackagePartProvider(scope: GlobalSearchScope): PackagePartProvider {
                val factory = testServices.compilerConfigurationProvider.getPackagePartProviderFactory(testModule)
                return factory(scope)
            }
        }
    )
    registerService(
        FirSealedClassInheritorsProcessorFactory::class.java,
        object : FirSealedClassInheritorsProcessorFactory() {
            override fun createSealedClassInheritorsProvider(): SealedClassInheritorsProvider {
                return SealedClassInheritorsProviderTestImpl()
            }
        }
    )
    registerService(
        ProjectModuleScopeProvider::class.java,
        object : ProjectModuleScopeProvider() {
            override fun getModuleLibrariesScope(sourceModule: SourceProjectModule): GlobalSearchScope {
                val libraries = sourceModule.dependenciesOfType<BinaryProjectModule>()
                val scopes = libraries.map { it.searchScope }.toList()
                if (scopes.isEmpty()) return GlobalSearchScope.EMPTY_SCOPE
                return GlobalSearchScope.union(scopes)
            }
        }
    )
    registerService(FirIdeResolveStateService::class.java)
    registerService(
        KotlinModificationTrackerFactory::class.java,
        KotlinStaticModificationTrackerFactory::class.java
    )
    registerService(KotlinDeclarationProviderFactory::class.java, KotlinStaticDeclarationProviderFactory(allKtFiles))
    registerService(KotlinPackageProviderFactory::class.java, KotlinStaticPackageProviderFactory(allKtFiles))
    registerService(ProjectStructureProvider::class.java, TestKotlinProjectStructureProvider(testServices))
    reRegisterJavaElementFinder(this)
}

private class TestKotlinProjectStructureProvider(testServices: TestServices) : ProjectStructureProvider() {
    private val moduleInfoProvider = testServices.projectModuleProvider
    override fun getProjectModuleForKtElement(element: PsiElement): ProjectModule {
        val containingFile = element.containingFile as KtFile
        return moduleInfoProvider.getModuleInfoByKtFile(containingFile)
    }
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
