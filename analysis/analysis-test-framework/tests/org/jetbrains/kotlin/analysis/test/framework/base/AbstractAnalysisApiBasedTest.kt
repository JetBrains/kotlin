/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.base

import com.intellij.openapi.util.Disposer
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.TestDataFile
import junit.framework.ComparisonFailure
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.analyzeCopy
import org.jetbrains.kotlin.analysis.project.structure.DanglingFileResolutionMode
import org.jetbrains.kotlin.analysis.test.framework.AnalysisApiTestDirectives
import org.jetbrains.kotlin.analysis.test.framework.TestWithDisposable
import org.jetbrains.kotlin.analysis.test.framework.project.structure.getKtFiles
import org.jetbrains.kotlin.analysis.test.framework.project.structure.ktModuleProvider
import org.jetbrains.kotlin.analysis.test.framework.services.ExpressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.services.ExpressionMarkersSourceFilePreprocessor
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.services.libraries.TestModuleCompiler
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.FrontendKind
import org.jetbrains.kotlin.analysis.test.framework.utils.SkipTestException
import org.jetbrains.kotlin.analysis.test.framework.utils.singleOrZeroValue
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.TestConfiguration
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.testConfiguration
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.model.singleOrZeroValue
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.ResultingArtifact
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerTest
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.services.impl.TemporaryDirectoryManagerImpl
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo
import java.io.IOException
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.nameWithoutExtension

/**
 * The base class for all Analysis API-based tests.
 *
 * There are three test entry points:
 * * [doTestByMainFile] – test cases with dedicated main file.
 * Supports everything from single-file cases to multi-platform multi-module multi-file cases
 * * [doTestByMainModuleAndOptionalMainFile] – test cases rather around modules than files
 * * [doTestByModuleStructure] – all other cases with fully custom logic
 *
 * Look at the KDoc of the corresponding method for more details.
 *
 * @see doTestByMainFile
 * @see doTestByMainModuleAndOptionalMainFile
 * @see doTestByModuleStructure
 */
abstract class AbstractAnalysisApiBasedTest : TestWithDisposable() {
    abstract val configurator: AnalysisApiTestConfigurator

    /**
     * Consider implementing this method if you can choose some main file in your test case.
     * It can be, for example, a file with caret.
     *
     * Examples of use cases:
     * * Collect diagnostics of the file
     * * Get an element at the caret and invoke some logic
     * * Do some operations on [mainFile] and dump a state of other files in [mainModule]
     *
     * Only one [KtFile] can be the main one.
     *
     * What is the main file?
     * Any of:
     * * It is a single file in [main][isMainModule] module
     * * It is a single file in the project
     * * The file has a selected expression
     * * The file has a caret
     * * The file name is equal to "main" or equal to the defined [AnalysisApiTestDirectives.MAIN_FILE_NAME]
     *
     * @see findMainFile
     * @see isMainFile
     * @see AnalysisApiTestDirectives.MAIN_FILE_NAME
     */
    protected open fun doTestByMainFile(mainFile: KtFile, mainModule: TestModule, testServices: TestServices) {
        throw UnsupportedOperationException(
            "The test case is not fully implemented. " +
                    "'${::doTestByMainFile.name}', '${::doTestByMainModuleAndOptionalMainFile.name}' or '${::doTestByModuleStructure.name}' should be overridden"
        )
    }

    /**
     * Consider implementing this method if you have logic around [TestModule],
     * or you don't always have a [mainFile] and have some custom logic for such exceptional cases
     * (e.g., the first file from [mainModule]).
     *
     * Examples of use cases:
     * * Find all declarations in the module
     * * Find a declaration by qualified name and invoke some logic
     * * Process all files in the module
     *
     * Only one [TestModule] can be the main one.
     *
     * What is the main module?
     * Any of:
     * * It is a single module
     * * It has a main file (see [doTestByMainFile] for details)
     * * The module has a defined [AnalysisApiTestDirectives.MAIN_MODULE] directive
     * * The module name is equal to [ModuleStructureExtractor.DEFAULT_MODULE_NAME]
     *
     * Use only if [doTestByMainFile] is not suitable for your use case
     *
     * @param mainFile a dedicated main file if it exists (see [findMainFile])
     *
     * @see findMainModule
     * @see isMainModule
     * @see AnalysisApiTestDirectives.MAIN_MODULE
     */
    protected open fun doTestByMainModuleAndOptionalMainFile(mainFile: KtFile?, mainModule: TestModule, testServices: TestServices) {
        doTestByMainFile(mainFile ?: error("The main file is not found"), mainModule, testServices)
    }

    /**
     * Consider implementing this method if you have logic around [TestModuleStructure].
     *
     * Examples of use cases:
     * * Find all files in all modules
     * * Find two declarations from different files and different modules and compare them
     *
     * Use only if [doTestByMainModuleAndOptionalMainFile] is not suitable for your use case
     */
    protected open fun doTestByModuleStructure(moduleStructure: TestModuleStructure, testServices: TestServices) {
        val (mainFile, mainModule) = findMainFileAndModule(moduleStructure, testServices)
        doTestByMainModuleAndOptionalMainFile(mainFile, mainModule, testServices)
    }

    private lateinit var testInfo: KotlinTestInfo

    protected lateinit var testDataPath: Path
        private set

    private var _testServices: TestServices? = null

    private var testServices: TestServices
        get() = _testServices ?: error("`_testServices` has not been initialized")
        set(value) {
            _testServices = value
        }

    protected open fun configureTest(builder: TestConfigurationBuilder) {
        configurator.configureTest(builder, disposable)
    }

    data class ModuleWithMainFile(val mainFile: KtFile?, val module: TestModule)

    protected fun findMainFileAndModule(moduleStructure: TestModuleStructure, testServices: TestServices): ModuleWithMainFile {
        findMainFileByMarkers(moduleStructure, testServices)?.let { return it }

        // We have this search not at the beginning of the function as we should prefer marked files to
        // a main module with one file
        val mainModule = findMainModule(testServices) ?: error("Cannot find the main test module")
        val mainFile = findMainFile(mainModule, testServices)
        return ModuleWithMainFile(mainFile, mainModule)
    }

    private fun findMainFileByMarkers(moduleStructure: TestModuleStructure, testServices: TestServices): ModuleWithMainFile? {
        return moduleStructure.modules.singleOrZeroValue(
            transformer = { module ->
                // We don't want to accept one-file modules without additional checks as it can be some intermediate
                // module that is not intended to be the main
                findMainFile(module, testServices, acceptSingleFileWithoutAdditionalChecks = false)?.let { mainFile ->
                    ModuleWithMainFile(mainFile, module)
                }
            },
            ambiguityValueRenderer = { "'${it.module.name}' with '${it.mainFile?.name}'" },
        )
    }

    protected fun findMainModule(testServices: TestServices): TestModule? {
        val testModules = testServices.moduleStructure.modules
        // One-module test, nothing to search
        testModules.singleOrNull()?.let { return it }

        return testModules.singleOrZeroValue(
            transformer = { module -> module.takeIf { isMainModule(module, testServices) } },
            ambiguityValueRenderer = { it.name },
        )
    }

    protected open fun isMainModule(module: TestModule, testServices: TestServices): Boolean {
        return AnalysisApiTestDirectives.MAIN_MODULE in module.directives ||
                // Multiplatform modules can have '-' delimiter for a platform definition
                module.name.substringBefore('-') == ModuleStructureExtractor.DEFAULT_MODULE_NAME
    }

    protected fun findMainFile(
        module: TestModule,
        testServices: TestServices,
        acceptSingleFileWithoutAdditionalChecks: Boolean = true,
    ): KtFile? {
        val ktFiles = testServices.ktModuleProvider.getKtFiles(module)
        if (acceptSingleFileWithoutAdditionalChecks) {
            // Simple case with one file
            ktFiles.singleOrNull()?.let { return it }
        }

        return ktFiles.singleOrZeroValue(
            transformer = { file -> file.takeIf { isMainFile(file, module, testServices) } },
            ambiguityValueRenderer = { it.name },
        )
    }

    protected val TestModule.mainFileName: String get() = directives.singleOrZeroValue(AnalysisApiTestDirectives.MAIN_FILE_NAME) ?: "main"

    protected open fun isMainFile(file: KtFile, module: TestModule, testServices: TestServices): Boolean {
        val expressionMarkerProvider = testServices.expressionMarkerProvider
        return expressionMarkerProvider.getCaretPositionOrNull(file) != null ||
                expressionMarkerProvider.getSelectedRangeOrNull(file) != null ||
                file.virtualFile.nameWithoutExtension == module.mainFileName
    }

    protected fun AssertionsService.assertEqualsToTestDataFileSibling(
        actual: String,
        extension: String = ".txt",
        testPrefix: String? = configurator.testPrefix,
    ) {
        val expectedFile = getTestDataFileSiblingPath(extension, testPrefix = testPrefix)
        assertEqualsToFile(expectedFile, actual)

        if (testPrefix != null) {
            val expectedFileWithoutPrefix = getTestDataFileSiblingPath(extension, testPrefix = null)
            if (expectedFile != expectedFileWithoutPrefix) {
                try {
                    assertEqualsToFile(expectedFileWithoutPrefix, actual)
                } catch (_: ComparisonFailure) {
                    return
                }

                throw AssertionError("\"$expectedFile\" has the same content as \"$expectedFileWithoutPrefix\". Delete the prefixed file.")
            }
        }
    }

    protected fun getTestDataFileSiblingPath(extension: String, testPrefix: String?): Path {
        val extensionWithDot = "." + extension.removePrefix(".")
        val baseName = testDataPath.nameWithoutExtension

        if (testPrefix != null) {
            val prefixedFile = testDataPath.resolveSibling("$baseName.$testPrefix$extensionWithDot")
            if (prefixedFile.exists()) {
                return prefixedFile
            }
        }

        return testDataPath.resolveSibling(baseName + extensionWithDot)
    }

    @OptIn(TestInfrastructureInternals::class)
    private val configure: TestConfigurationBuilder.() -> Unit = {
        globalDefaults {
            frontend = FrontendKinds.FIR
            targetPlatform = JvmPlatforms.defaultJvmPlatform
            dependencyKind = DependencyKind.Source
        }

        assertions = JUnit5Assertions
        useAdditionalService<TemporaryDirectoryManager>(::TemporaryDirectoryManagerImpl)

        useDirectives(*AbstractKotlinCompilerTest.defaultDirectiveContainers.toTypedArray())
        useDirectives(JvmEnvironmentConfigurationDirectives)
        useDirectives(TestModuleCompiler.Directives)


        useSourcePreprocessor(::ExpressionMarkersSourceFilePreprocessor)
        useAdditionalService { ExpressionMarkerProvider() }
        useDirectives(ExpressionMarkerProvider.Directives)

        registerAnalysisApiBaseTestServices(disposable, configurator)
        configureTest(this)

        startingArtifactFactory = { ResultingArtifact.Source() }
        this.testInfo = this@AbstractAnalysisApiBasedTest.testInfo
    }

    protected fun runTest(@TestDataFile path: String) {
        testDataPath = configurator.computeTestDataPath(Paths.get(path))
        val testConfiguration = createTestConfiguration()
        testServices = testConfiguration.testServices
        val moduleStructure = createModuleStructure(testConfiguration)

        if (configurator.analyseInDependentSession && isDependentModeDisabledForTheTest()) {
            return
        }

        if (configurator.frontendKind == FrontendKind.Fe10 && isFe10DisabledForTheTest() ||
            configurator.frontendKind == FrontendKind.Fir && isFirDisabledForTheTest()
        ) {
            return
        }

        try {
            prepareToTheAnalysis(testConfiguration)
        } catch (_: SkipTestException) {
            return
        }


        doTestByModuleStructure(moduleStructure, testServices)
    }

    @AfterEach
    fun cleanupTemporaryDirectories() {
        try {
            _testServices?.temporaryDirectoryManager?.cleanupTemporaryDirectories()
        } catch (e: IOException) {
            println("Failed to clean temporary directories: ${e.message}\n${e.stackTrace}")
        }
    }

    private fun createTestConfiguration(): TestConfiguration {
        val testConfiguration = testConfiguration(testDataPath.toString(), configure)
        Disposer.register(disposable, testConfiguration.rootDisposable)
        return testConfiguration
    }

    private fun createModuleStructure(testConfiguration: TestConfiguration): TestModuleStructure {
        val moduleStructure = testConfiguration.moduleStructureExtractor.splitTestDataByModules(
            testDataPath.toString(),
            testConfiguration.directives,
        )

        testServices.register(TestModuleStructure::class, moduleStructure)
        return moduleStructure
    }

    private fun prepareToTheAnalysis(testConfiguration: TestConfiguration) {
        val moduleStructure = testServices.moduleStructure
        val dependencyProvider = DependencyProviderImpl(testServices, moduleStructure.modules)
        testServices.registerDependencyProvider(dependencyProvider)

        testConfiguration.preAnalysisHandlers.forEach { preprocessor -> preprocessor.preprocessModuleStructure(moduleStructure) }
        testConfiguration.preAnalysisHandlers.forEach { preprocessor -> preprocessor.prepareSealedClassInheritors(moduleStructure) }

        moduleStructure.modules.forEach { module ->
            val files = testServices.ktModuleProvider.getModuleFiles(module)
            configurator.prepareFilesInModule(files, module, testServices)
        }
    }

    private fun isDependentModeDisabledForTheTest(): Boolean =
        AnalysisApiTestDirectives.DISABLE_DEPENDED_MODE in testServices.moduleStructure.allDirectives

    private fun isFe10DisabledForTheTest(): Boolean =
        AnalysisApiTestDirectives.IGNORE_FE10 in testServices.moduleStructure.allDirectives

    private fun isFirDisabledForTheTest(): Boolean =
        AnalysisApiTestDirectives.IGNORE_FIR in testServices.moduleStructure.allDirectives

    protected fun <R> analyseForTest(contextElement: KtElement, action: KtAnalysisSession.(KtElement) -> R): R {
        return if (configurator.analyseInDependentSession) {
            val originalContainingFile = contextElement.containingKtFile
            val fileCopy = originalContainingFile.copy() as KtFile

            analyzeCopy(fileCopy, DanglingFileResolutionMode.IGNORE_SELF) {
                action(PsiTreeUtil.findSameElementInCopy<KtElement>(contextElement, fileCopy))
            }
        } else {
            analyze(contextElement, action = { action(contextElement) })
        }
    }

    @BeforeEach
    fun initTestInfo(testInfo: TestInfo) {
        this.testInfo = KotlinTestInfo(
            className = testInfo.testClass.orElseGet(null)?.name ?: "_undefined_",
            methodName = testInfo.testMethod.orElseGet(null)?.name ?: "_testUndefined_",
            tags = testInfo.tags
        )
    }
}