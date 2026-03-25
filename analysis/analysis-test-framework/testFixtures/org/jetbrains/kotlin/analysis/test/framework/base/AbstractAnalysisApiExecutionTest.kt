/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.base

import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.TestMetadata
import org.jetbrains.kotlin.test.services.TestServices
import org.junit.jupiter.api.extension.*
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists

/**
 * A base test for non-generated (manual) tests of the Analysis API.
 *
 * Prefer using generated tests for common use-cases.
 * Add manual tests only when you need to cover a very special case.
 *
 * Test method names must be equal to file names in the test data, without the file extension. E.g.,
 *
 * ```
 * @Test
 * fun foo() {
 *     // Tests 'foo.kt', or, if it doesn't exist, 'foo.kts'
 * }
 * ```
 *
 * If needed, tests may have parameters of the following types:
 *   - [KtFile] (the main file)
 *   - [KtTestModule] (the main test module)
 *   - [TestServices]
 *
 * @param testDirPathString The path to the test data, relative to the project root.
 */
@ExtendWith(AnalysisApiExecutionTestExtension::class)
abstract class AbstractAnalysisApiExecutionTest(val testDirPathString: String) : AbstractAnalysisApiBasedTest() {
    @Deprecated("Handled by the test infrastructure. Avoid calling directly")
    fun performTest(path: String, block: (TestServices, KtFile?, KtTestModule?) -> Unit) {
        runTest(path) { testServices ->
            val moduleWithMainFile = findMainFileAndModule(testServices)
            block(testServices, moduleWithMainFile?.mainFile, moduleWithMainFile?.module)
        }
    }
}

/**
 * Marks a field for automatic injection of the [AnalysisApiExecutionTestEnvironment].
 *
 * The annotated field must be a `private lateinit var` of type [AnalysisApiExecutionTestEnvironment].
 * It is set before test execution and cleared after.
 */
@Target(AnnotationTarget.FIELD)
annotation class AnalysisApiTestEnvironmentStorage

/**
 * Holds the test environment created during execution of an [AbstractAnalysisApiExecutionTest].
 *
 * Instances are injected into fields annotated with [AnalysisApiTestEnvironmentStorage].
 * Alternatively, individual components can be received as test method parameters.
 */
class AnalysisApiExecutionTestEnvironment(
    val testServices: TestServices,
    val mainFile: KtFile?,
    val mainModule: KtTestModule?
)

private class AnalysisApiExecutionTestExtension : BeforeTestExecutionCallback, AfterTestExecutionCallback, ParameterResolver {
    private companion object {
        private val SUPPORTED_PARAMETER_TYPES = listOf(
            TestServices::class.java,
            KtFile::class.java,
            KtTestModule::class.java,
        )
    }

    private var cachedTestEnvironment = ThreadLocal<AnalysisApiExecutionTestEnvironment>()

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
        val parameterType = parameterContext.parameter.type
        return SUPPORTED_PARAMETER_TYPES.any { it.isAssignableFrom(parameterType) }
    }

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any? {
        val cachedEnvironment = this.cachedTestEnvironment.get() ?: error("Test environment is not cached yet")
        val parameterType = parameterContext.parameter.type
        return when {
            parameterType.isAssignableFrom(TestServices::class.java) -> cachedEnvironment.testServices
            parameterType.isAssignableFrom(KtFile::class.java) -> cachedEnvironment.mainFile
            parameterType.isAssignableFrom(KtTestModule::class.java) -> cachedEnvironment.mainModule
            else -> error("Unsupported parameter type $parameterType")
        }
    }

    override fun beforeTestExecution(context: ExtensionContext) {
        val testInstance = context.requiredTestInstance as AbstractAnalysisApiExecutionTest

        val testNamePath = computeTestFilePath(context.requiredTestMethod)
        val testFilePath = getTestFilePath(testInstance.testDirPathString, testNamePath)

        @Suppress("DEPRECATION")
        testInstance.performTest(testFilePath.toString()) { testServices, mainFile, mainModule ->
            val testEnvironment = AnalysisApiExecutionTestEnvironment(
                testServices,
                mainFile,
                mainModule
            )

            require(cachedTestEnvironment.get() == null)
            cachedTestEnvironment.set(testEnvironment)

            context.updateTestEnvironmentStorage(testEnvironment)
        }
    }

    private fun computeTestFilePath(testMethod: Method): String {
        val testMetadataAnnotation = testMethod.getAnnotation(TestMetadata::class.java)
        if (testMetadataAnnotation != null) {
            return testMetadataAnnotation.value
        }

        // The test name itself can contain a path. However, '/' cannot appear in JVM method names, so we use spaces instead.
        // The letter-digit filtering allows including further information in the test name (e.g., an output file extension).
        return testMethod.name
            .split(' ')
            .takeWhile { it.first().isLetterOrDigit() }
            .joinToString("/")
    }

    override fun afterTestExecution(context: ExtensionContext) {
        cachedTestEnvironment.remove()
        context.updateTestEnvironmentStorage(null)
    }

    private fun ExtensionContext.updateTestEnvironmentStorage(newValue: AnalysisApiExecutionTestEnvironment?) {
        val testInstance = requiredTestInstance

        tailrec fun update(klass: Class<*>) {
            for (field in klass.declaredFields) {
                if (!field.isAnnotationPresent(AnalysisApiTestEnvironmentStorage::class.java)) {
                    continue
                }

                require(field.type == AnalysisApiExecutionTestEnvironment::class.java) {
                    "Field '${field.name}' annotated with @AnalysisApiTestEnvironmentStorage must have type " +
                            "'AnalysisApiExecutionTestEnvironment', but has type '${field.type.name}'"
                }

                require(Modifier.isPrivate(field.modifiers)) {
                    "Field '${field.name}' annotated with @AnalysisApiTestEnvironmentStorage must be private to avoid exposing " +
                            "the test environment to the entire test hierarchy"
                }

                field.isAccessible = true

                val currentValue = field.get(testInstance)
                require((currentValue == null) xor (newValue == null))
                field.set(testInstance, newValue)
            }

            val superclass = klass.superclass ?: return
            update(superclass)
        }

        update(testInstance.javaClass)
    }

    private fun getTestFilePath(testDirPathString: String, testFileName: String): Path {
        fun tryPath(extension: String): Path? {
            return Paths.get(testDirPathString, testFileName + extension).takeIf { it.exists() }
        }

        return tryPath("")
            ?: tryPath(".kt")
            ?: tryPath(".kts")
            ?: tryPath(".repl.kts")
            ?: tryPath(".kotlin_builtins")
            ?: error("Cannot find test file $testFileName.kt(s) in $testDirPathString")
    }
}
