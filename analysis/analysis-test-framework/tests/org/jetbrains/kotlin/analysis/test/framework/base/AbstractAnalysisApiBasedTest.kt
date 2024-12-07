/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.base

import com.intellij.openapi.util.Disposer
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.analyzeCopy
import org.jetbrains.kotlin.analysis.api.projectStructure.KaDanglingFileResolutionMode
import org.jetbrains.kotlin.analysis.test.framework.AnalysisApiTestDirectives
import org.jetbrains.kotlin.analysis.test.framework.TestWithDisposable
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.ktTestModuleStructure
import org.jetbrains.kotlin.analysis.test.framework.services.ExpressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.services.ExpressionMarkersSourceFilePreprocessor
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.services.libraries.TestModuleCompiler
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.FrontendKind
import org.jetbrains.kotlin.analysis.test.framework.utils.SkipTestException
import org.jetbrains.kotlin.analysis.test.framework.utils.singleOrZeroValue
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.TestConfiguration
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.testConfiguration
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.model.Directive
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.directives.model.StringDirective
import org.jetbrains.kotlin.test.directives.model.singleOrZeroValue
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.ResultingArtifact
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerTest
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.services.impl.TemporaryDirectoryManagerImpl
import org.jetbrains.kotlin.types.AbstractTypeChecker
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
 *
 * - [doTestByMainFile] – test cases with a dedicated main file.
 *   Supports everything from single-file cases to multi-platform multi-module multi-file cases.
 * - [doTestByMainModuleAndOptionalMainFile] – test cases rather around modules than files
 * - [doTest] – all other cases with fully custom logic
 *
 * Look at the KDoc of the corresponding method for more details.
 *
 * @see doTestByMainFile
 * @see doTestByMainModuleAndOptionalMainFile
 * @see doTest
 */
abstract class AbstractAnalysisApiBasedTest : TestWithDisposable() {
    abstract val configurator: AnalysisApiTestConfigurator

    /**
     * Consider implementing this method if you can choose some main file in your test case. It can be, for example, a file with a caret.
     *
     * Examples of use cases:
     *
     * - Collect diagnostics of the file
     * - Get an element at the caret and invoke some logic
     * - Do some operations on [mainFile] and dump a state of other files in [mainModule]
     *
     * Only one [KtFile] can be the main one.
     *
     * The main file is selected based on the following rules:
     *
     * - A single file in the [main][isMainModule] module
     * - A single file in the project
     * - The file has a selected expression
     * - The file has a caret
     * - The file name is equal to "main" or equal to the defined [AnalysisApiTestDirectives.MAIN_FILE_NAME]
     *
     * @see findMainFile
     * @see isMainFile
     * @see AnalysisApiTestDirectives.MAIN_FILE_NAME
     */
    protected open fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        throw UnsupportedOperationException(
            "The test case is not fully implemented. " +
                    "'${::doTestByMainFile.name}', '${::doTestByMainModuleAndOptionalMainFile.name}' or '${::doTest.name}' should be overridden"
        )
    }

    /**
     * Consider implementing this method if you have logic around [KtTestModule], or you don't always have a [mainFile] and have some custom
     * logic for such exceptional cases (e.g., taking the first file from [mainModule]).
     *
     * Examples of use cases:
     *
     * - Find all declarations in the module
     * - Find a declaration by qualified name and invoke some logic
     * - Process all files in the module
     *
     * Only one [KtTestModule] can be the main one.
     *
     * The main module is selected based on the following rules:
     *
     * - It is the only module
     * - It has a main file (see [doTestByMainFile] for details)
     * - The module has a defined [AnalysisApiTestDirectives.MAIN_MODULE] directive
     * - The module name is equal to [ModuleStructureExtractor.DEFAULT_MODULE_NAME]
     *
     * Use [doTestByMainModuleAndOptionalMainFile] only if [doTestByMainFile] is not suitable for your use case.
     *
     * @param mainFile a dedicated main file if it exists (see [findMainFile])
     *
     * @see findMainModule
     * @see isMainModule
     * @see AnalysisApiTestDirectives.MAIN_MODULE
     */
    protected open fun doTestByMainModuleAndOptionalMainFile(mainFile: KtFile?, mainModule: KtTestModule, testServices: TestServices) {
        doTestByMainFile(mainFile ?: error("The main file is not found"), mainModule, testServices)
    }

    /**
     * Consider implementing this method if your test logic needs the whole
     * [KtTestModuleStructure][org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModuleStructure].
     *
     * Examples of use cases:
     *
     * - Find all files in all modules
     * - Find two declarations from different files and different modules and compare them
     *
     * The [KtTestModuleStructure][org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModuleStructure] can be accessed via
     * [ktTestModuleStructure] on [testServices].
     *
     * Use only if [doTestByMainModuleAndOptionalMainFile] is not suitable for your use case.
     */
    protected open fun doTest(testServices: TestServices) {
        val (mainFile, mainModule) = findMainFileAndModule(testServices)
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

    data class ModuleWithMainFile(val mainFile: KtFile?, val module: KtTestModule)

    protected fun findMainFileAndModule(testServices: TestServices): ModuleWithMainFile {
        findMainFileByMarkers(testServices)?.let { return it }

        // We have this search not at the beginning of the function as we should prefer marked files to
        // a main module with one file
        val mainModule = findMainModule(testServices) ?: error("Cannot find the main test module")
        val mainFile = findMainFile(mainModule, testServices)
        return ModuleWithMainFile(mainFile, mainModule)
    }

    private fun findMainFileByMarkers(testServices: TestServices): ModuleWithMainFile? {
        return testServices.ktTestModuleStructure.mainModules.mapNotNull { module ->
            // We don't want to accept one-file modules without additional checks as it can be some intermediate
            // module that is not intended to be the main
            findMainFile(module, testServices, acceptSingleFileWithoutAdditionalChecks = false)?.let { mainFile ->
                ModuleWithMainFile(mainFile, module)
            }
        }.singleOrNull()
    }

    protected fun findMainModule(testServices: TestServices): KtTestModule? {
        val modules = testServices.ktTestModuleStructure.mainModules
        // One-module test, nothing to search
        modules.singleOrNull()?.let { return it }

        return modules.singleOrZeroValue(
            transformer = { module -> module.takeIf { isMainModule(module, testServices) } },
            ambiguityValueRenderer = { it.testModule.name },
        )
    }

    protected open fun isMainModule(ktTestModule: KtTestModule, testServices: TestServices): Boolean {
        return AnalysisApiTestDirectives.MAIN_MODULE in ktTestModule.testModule.directives ||
                // Multiplatform modules can have '-' delimiter for a platform definition
                ktTestModule.testModule.name.substringBefore('-') == ModuleStructureExtractor.DEFAULT_MODULE_NAME
    }

    protected fun findMainFile(
        ktTestModule: KtTestModule,
        testServices: TestServices,
        acceptSingleFileWithoutAdditionalChecks: Boolean = true,
    ): KtFile? {
        val ktFiles = ktTestModule.ktFiles
        if (acceptSingleFileWithoutAdditionalChecks) {
            // Simple case with one file
            ktFiles.singleOrNull()?.let { return it }
        }

        return ktFiles.singleOrZeroValue(
            transformer = { file -> file.takeIf { isMainFile(file, ktTestModule, testServices) } },
            ambiguityValueRenderer = { it.name },
        )
    }

    protected val TestModule.mainFileName: String get() = directives.singleOrZeroValue(AnalysisApiTestDirectives.MAIN_FILE_NAME) ?: "main"

    protected open fun isMainFile(file: KtFile, ktTestModule: KtTestModule, testServices: TestServices): Boolean {
        val expressionMarkerProvider = testServices.expressionMarkerProvider
        return expressionMarkerProvider.getCaretPositionOrNull(file) != null ||
                expressionMarkerProvider.getSelectedRangeOrNull(file) != null ||
                file.virtualFile.nameWithoutExtension == ktTestModule.testModule.mainFileName
    }

    /**
     * Checks whether the [actual] string matches the content of the test output file.
     *
     * Check the [assertEqualsToTestDataFileSibling] overload accepting a prefix list for more information.
     */
    protected fun AssertionsService.assertEqualsToTestDataFileSibling(
        actual: String,
        extension: String = ".txt",
        testPrefix: String? = configurator.testPrefix,
    ) {
        assertEqualsToTestDataFileSibling(actual, extension, listOfNotNull(testPrefix))
    }

    /**
     * Checks whether the [actual] string matches the content of the test output file.
     *
     * If a non-empty list of [testPrefixes] is specified, the function will firstly check whether test output file with any of the
     * specified prefixes exist. If so, it will check the [actual] content against that file (the first prefix has the highest priority).
     * Also, if files with latter prefixes, or if the non-prefixed file contains the same output, an assertion error is raised.
     *
     * If no prefixes are specified, or if no prefixed files exist, the function compares [actual] against the non-prefixed (default)
     * test output file.
     *
     * If none of the test output files exist, the function creates an output file, writes the content of [actual] to it, and throws
     * an exception.
     */
    protected fun AssertionsService.assertEqualsToTestDataFileSibling(
        actual: String,
        extension: String = ".txt",
        testPrefixes: List<String>,
    ) {
        val expectedFiles = buildList {
            testPrefixes.mapNotNullTo(this) { findPrefixedTestDataSibling(extension, it) }
            add(getDefaultTestDataSibling(extension))
        }

        val mainExpectedFile = expectedFiles.first()
        val otherExpectedFiles = expectedFiles.drop(1)

        assertEqualsToFile(mainExpectedFile, actual)

        for (otherExpectedFile in otherExpectedFiles) {
            if (doesEqualToFile(otherExpectedFile.toFile(), actual)) {
                throw AssertionError("\"$mainExpectedFile\" has the same content as \"$otherExpectedFile\". Delete the prefixed file.")
            }
        }
    }

    /**
     * Returns the test output file with a [testPrefix] if it exists, of the non-prefixed (default) test output file.
     */
    protected fun getTestDataSibling(extension: String = "txt", testPrefix: String? = configurator.testPrefix): Path {
        if (testPrefix != null) {
            findPrefixedTestDataSibling(extension, testPrefix)?.let { return it }
        }

        return getDefaultTestDataSibling(extension)
    }

    /**
     * Returns the test output file with a [testPrefix] if it exists, or `null` otherwise.
     */
    private fun findPrefixedTestDataSibling(extension: String = "txt", testPrefix: String): Path? {
        val extensionWithDot = "." + extension.removePrefix(".")
        val baseName = testDataPath.nameWithoutExtension

        val prefixedFile = testDataPath.resolveSibling("$baseName.$testPrefix$extensionWithDot")
        return prefixedFile.takeIf { it.exists() }
    }

    /**
     * Returns the non-prefixed the test output file, even if it does not exist.
     */
    private fun getDefaultTestDataSibling(extension: String = "txt"): Path {
        val extensionWithDot = "." + extension.removePrefix(".")
        val baseName = testDataPath.nameWithoutExtension
        return testDataPath.resolveSibling(baseName + extensionWithDot)
    }

    @OptIn(TestInfrastructureInternals::class)
    private val configure: TestConfigurationBuilder.() -> Unit = {
        globalDefaults {
            frontend = FrontendKinds.FIR
            targetPlatform = configurator.defaultTargetPlatform
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

        AbstractTypeChecker.RUN_SLOW_ASSERTIONS = true
    }

    protected fun runTest(@TestDataFile path: String) {
        testDataPath = configurator.computeTestDataPath(Paths.get(path))
        val testConfiguration = createTestConfiguration()
        testServices = testConfiguration.testServices
        createAndRegisterTestModuleStructure(testConfiguration)

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

        doTest(testServices)
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

    private fun createAndRegisterTestModuleStructure(testConfiguration: TestConfiguration) {
        val moduleStructure = testConfiguration.moduleStructureExtractor.splitTestDataByModules(
            testDataPath.toString(),
            testConfiguration.directives,
        )

        testServices.register(TestModuleStructure::class, moduleStructure)
    }

    private fun prepareToTheAnalysis(testConfiguration: TestConfiguration) {
        val moduleStructure = testServices.moduleStructure
        val dependencyProvider = DependencyProviderImpl(testServices, moduleStructure.modules)
        testServices.registerDependencyProvider(dependencyProvider)

        testConfiguration.preAnalysisHandlers.forEach { preprocessor -> preprocessor.preprocessModuleStructure(moduleStructure) }
        testConfiguration.preAnalysisHandlers.forEach { preprocessor -> preprocessor.prepareSealedClassInheritors(moduleStructure) }

        testServices.ktTestModuleStructure.mainModules.forEach { ktTestModule ->
            configurator.prepareFilesInModule(ktTestModule, testServices)
        }
    }

    private fun isDependentModeDisabledForTheTest(): Boolean =
        AnalysisApiTestDirectives.DISABLE_DEPENDED_MODE in testServices.moduleStructure.allDirectives

    private fun isFe10DisabledForTheTest(): Boolean =
        AnalysisApiTestDirectives.IGNORE_FE10 in testServices.moduleStructure.allDirectives

    private fun isFirDisabledForTheTest(): Boolean =
        AnalysisApiTestDirectives.IGNORE_FIR in testServices.moduleStructure.allDirectives

    protected fun <T : Directive> RegisteredDirectives.findSpecificDirective(
        commonDirective: T,
        k1Directive: T,
        k2Directive: T,
    ): T? = commonDirective.takeIf { it in this }
        ?: k1Directive.takeIf { configurator.frontendKind == FrontendKind.Fe10 && it in this }
        ?: k2Directive.takeIf { configurator.frontendKind == FrontendKind.Fir && it in this }

    protected fun <R> analyseForTest(contextElement: KtElement, action: KaSession.(KtElement) -> R): R {
        return if (configurator.analyseInDependentSession) {
            val originalContainingFile = contextElement.containingKtFile
            val fileCopy = originalContainingFile.copy() as KtFile

            analyzeCopy(fileCopy, KaDanglingFileResolutionMode.IGNORE_SELF) {
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

    fun RegisteredDirectives.suppressIf(suppressionDirective: StringDirective, filter: (Throwable) -> Boolean, action: () -> Unit) {
        val hasSuppressionDirective = suppressionDirective in this
        var exception: Throwable? = null
        try {
            action()
        } catch (e: Throwable) {
            exception = e
        }

        if (exception != null) {
            if (!filter(exception) || !hasSuppressionDirective) {
                throw exception
            }

            return
        } else if (hasSuppressionDirective) {
            throw AssertionError("'${suppressionDirective.name}' directive present but no exception thrown. Please remove directive")
        }
    }
}