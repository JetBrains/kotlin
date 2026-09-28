/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.buildtools.forward.tests

import org.jetbrains.kotlin.buildtools.api.*
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_RETURN_VALUE_CHECKER
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.enums.ReturnValueCheckerMode
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import org.jetbrains.kotlin.buildtools.forward.tests.SmokeCompilationTest.Companion.jvmNonIncrementalCompilationOperation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.io.path.writeText

/**
 * Since Kotlin 2.5, `-Xreturn-value-checker` defaults to the new `default` value (KT-87295), which the
 * `ReturnValueCheckerMode` enum of older API versions cannot represent.
 */
@OptIn(ExperimentalCompilerArgument::class, ExperimentalBuildToolsApi::class)
internal class ReturnValueCheckerForwardCompatibilityTest : BaseCompilationTest() {

    @DisplayName("Return value checker is enabled by default when the argument is not set")
    @Test
    fun testCheckerEnabledByDefault() {
        val logger = CapturingLogger()

        val result = compile(createOperation(ANNOTATED_SOURCE), logger)

        assertEquals(CompilationResult.COMPILATION_SUCCESS, result)
        assertTrue(logger.warnings.any { it.contains(UNUSED_RETURN_VALUE_WARNING, ignoreCase = true) }) {
            "Expected an unused return value warning, but warnings were: ${logger.warnings}"
        }
    }

    @DisplayName("Enum values known to the loaded API are still accepted by the implementation")
    @Test
    fun testOldApiEnumValuesStillAccepted() {
        for (mode in ReturnValueCheckerMode.entries.filterNot { it.name == IMPL_ONLY_MODE }) {
            val logger = CapturingLogger()
            val operation = createOperation(PLAIN_SOURCE) {
                compilerArguments[X_RETURN_VALUE_CHECKER] = mode
            }

            val result = compile(operation, logger)

            assertEquals(CompilationResult.COMPILATION_SUCCESS, result) { "Compilation failed for mode $mode" }
            val expectWarning = mode == ReturnValueCheckerMode.FULL
            assertEquals(expectWarning, logger.warnings.any { it.contains(UNUSED_RETURN_VALUE_WARNING, ignoreCase = true) }) {
                "Unexpected unused return value warning state for mode $mode, warnings were: ${logger.warnings}"
            }
        }
    }

    @DisplayName("The new 'default' raw argument value is accepted through the old API")
    @Test
    fun testDefaultRawStringValueAccepted() {
        val logger = CapturingLogger()
        val operation = createOperation(ANNOTATED_SOURCE, rawArguments = listOf(RETURN_VALUE_CHECKER_DEFAULT_ARGUMENT))

        val result = compile(operation, logger)

        assertEquals(CompilationResult.COMPILATION_SUCCESS, result)
        assertTrue(logger.warnings.any { it.contains(UNUSED_RETURN_VALUE_WARNING, ignoreCase = true) }) {
            "Expected an unused return value warning, but warnings were: ${logger.warnings}"
        }
    }

    @DisplayName("The impl-only 'default' enum value is reported when read through the old API")
    @Test
    fun testDefaultValueNotRepresentableInOldApiIsReported() {
        assumeTrue(
            ReturnValueCheckerMode.entries.none { it.name == IMPL_ONLY_MODE },
            "The loaded kotlin-build-tools-api already declares ReturnValueCheckerMode.$IMPL_ONLY_MODE"
        )

        val operation = createOperation(PLAIN_SOURCE, rawArguments = listOf(RETURN_VALUE_CHECKER_DEFAULT_ARGUMENT))

        val exception = assertThrows<CompilerArgumentsParseException> {
            operation.compilerArguments[X_RETURN_VALUE_CHECKER]
        }

        val message = exception.message.orEmpty()
        assertTrue("Value '$IMPL_ONLY_MODE' of ReturnValueCheckerMode is not available in the loaded kotlin-build-tools-api; it exists in kotlin-build-tools-impl" in message) {
            "Message is not the one which is expected: $message"
        }
    }

    private fun createOperation(
        sourceCode: String,
        rawArguments: List<String> = emptyList(),
        additionalConfiguration: JvmCompilationOperation.Builder.() -> Unit = {},
    ): JvmCompilationOperation {
        val sources = listOf(workingDirectory.resolve("Sources.kt").also {
            it.writeText(sourceCode)
        })
        return jvmNonIncrementalCompilationOperation(
            sources,
            workingDirectory.resolve("classes"),
            rawArguments = rawArguments,
            additionalConfiguration = additionalConfiguration,
        )
    }

    private fun compile(operation: JvmCompilationOperation, logger: KotlinLogger): CompilationResult =
        toolchain.createBuildSession().use {
            it.executeOperation(operation, toolchain.createInProcessExecutionPolicy(), logger)
        }

    private class CapturingLogger : KotlinLogger {
        override val isDebugEnabled = true
        val warnings = CopyOnWriteArrayList<String>()

        override fun debug(msg: String) {}
        override fun error(msg: String, throwable: Throwable?) {}
        override fun info(msg: String) {}
        override fun lifecycle(msg: String) {}
        override fun warn(msg: String, throwable: Throwable?) {
            warnings.add(msg)
        }
    }

    private companion object {
        const val IMPL_ONLY_MODE = "DEFAULT"
        const val RETURN_VALUE_CHECKER_DEFAULT_ARGUMENT = "-Xreturn-value-checker=default"
        const val UNUSED_RETURN_VALUE_WARNING = "unused return value"

        val ANNOTATED_SOURCE = """
            @MustUseReturnValues
            object Annotated {
                fun annotated(): String = ""
            }

            fun test() {
                Annotated.annotated()
            }
        """.trimIndent()

        val PLAIN_SOURCE = """
            fun basic(): String = ""

            fun test() {
                basic()
            }
        """.trimIndent()
    }
}
