/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.buildtools.tests

import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.DeprecatedCompilerArgument
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.RemovedCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.CLASSPATH
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.NO_REFLECT
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.NO_STDLIB
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.io.path.absolutePathString
import kotlin.io.path.toPath
import kotlin.io.path.writeText

@OptIn(ExperimentalCompilerArgument::class, ExperimentalBuildToolsApi::class)
internal class CompilerArgumentCompatibilityTest : BaseCompilationTest() {

    @OptIn(RemovedCompilerArgument::class)
    @DisplayName("Removed argument throws exception with correct message")
    @Test
    fun testRemovedArgument() {
        val removedArgument = JvmCompilerArguments.X_IR_INLINER
        val jvmOperation = createSimpleJvmOperation()
        jvmOperation.compilerArguments[removedArgument] = true

        val exception = assertThrows<IllegalStateException> {
            toolchain.createBuildSession().use {
                it.executeOperation(jvmOperation, toolchain.createInProcessExecutionPolicy())
            }
        }

        val message = exception.message ?: ""
        assertTrue(message.contains("Compiler parameter not recognized: ${removedArgument.id}")) {
            "Expected message to contain argument id, but was: $message"
        }
        assertTrue(message.contains("the argument was removed")) {
            "Expected message to mention 'removed', but was: $message"
        }
    }

    @DisplayName("Newly introduced argument compiles successfully")
    @Test
    fun testNewlyIntroducedArgument() {
        val introducedArgument = CommonCompilerArguments.X_DETAILED_PERF
        val jvmOperation = createSimpleJvmOperation()
        jvmOperation.compilerArguments[introducedArgument] = true

        val result = toolchain.createBuildSession().use {
            it.executeOperation(jvmOperation, toolchain.createInProcessExecutionPolicy())
        }

        assertEquals(CompilationResult.COMPILATION_SUCCESS, result)
    }

    @OptIn(DeprecatedCompilerArgument::class)
    @DisplayName("Deprecated argument compiles successfully and emits warning")
    @Test
    fun testDeprecatedArgument() {
        val deprecatedArgument = CommonCompilerArguments.X_USE_FIR_EXPERIMENTAL_CHECKERS
        val logger = CapturingLogger()
        val jvmOperation = createSimpleJvmOperation()
        jvmOperation.compilerArguments[deprecatedArgument] = true

        val result = toolchain.createBuildSession().use {
            it.executeOperation(jvmOperation, toolchain.createInProcessExecutionPolicy(), logger)
        }

        assertEquals(CompilationResult.COMPILATION_SUCCESS, result)
        assertTrue(logger.warnings.any { it.contains("is deprecated and will be removed in a future release") }) {
            "Expected deprecation warning, but warnings were: ${logger.warnings}"
        }
    }

    private fun createSimpleJvmOperation(): JvmCompilationOperation {
        val sources = listOf(workingDirectory.resolve("Foo.kt").also {
            it.writeText("class Foo")
        })
        val destination = workingDirectory.resolve("classes")
        return toolchain.jvm.createJvmCompilationOperation(sources, destination).apply {
            compilerArguments[NO_REFLECT] = true
            compilerArguments[NO_STDLIB] = true
            compilerArguments[CLASSPATH] =
                KotlinVersion::class.java.protectionDomain.codeSource.location.toURI().toPath().absolutePathString()
        }
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
}
