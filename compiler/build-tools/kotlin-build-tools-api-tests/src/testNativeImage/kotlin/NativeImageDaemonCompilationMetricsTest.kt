/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.buildtools.tests.compilation

import org.jetbrains.kotlin.buildtools.api.BuildOperation.Companion.METRICS_COLLECTOR
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.tests.TestBuildMetricsCollector
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertOutputs
import org.jetbrains.kotlin.buildtools.tests.compilation.model.jvmProject
import org.jetbrains.kotlin.buildtools.tests.compilation.util.btaClassloader
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.file.Path

@DisplayName("Compilation metrics with the native image daemon execution policy")
class NativeImageDaemonCompilationMetricsTest : BaseCompilationTest() {
    val nativeImageDistPath: Path = ForTestCompileRuntime.kotlinNativeImageDistForTests().toPath()

    @Test
    @DisplayName("Compiler metrics are reported to the metrics collector")
    @TestMetadata("basic-multimodule-project/module-1")
    fun testCompilerMetricsAreReported() {
        val kotlinToolchains = KotlinToolchains.loadImplementation(btaClassloader)
        runSingleShotNativeImageDaemonTest(kotlinToolchains, nativeImageDistPath) { daemonPolicy, _ ->
            jvmProject(kotlinToolchains to daemonPolicy) {
                val module = module("basic-multimodule-project/module-1")
                val metricsCollector = TestBuildMetricsCollector()

                module.compile(
                    compilationConfigAction = {
                        it[METRICS_COLLECTOR] = metricsCollector
                    }
                ) {
                    assertOutputs("FooKt.class", "Bar.class", "BazKt.class")
                }

                val actualMetricNames = metricsCollector.all().map { it.name }.toSet()
                val missingMetricNames = expectedCompilerMetricNames - actualMetricNames
                assertTrue(missingMetricNames.isEmpty()) {
                    "The native image daemon did not report the expected compiler metrics.\nMissing: $missingMetricNames\nActual: $actualMetricNames"
                }
            }
        }
    }

    private companion object {
        private const val SOURCES_ROUND_COMPILER_TIME =
            "Run compilation -> Sources compilation round -> Compiler time"

        private val expectedCompilerMetricNames = setOf(
            "$SOURCES_ROUND_COMPILER_TIME -> Compiler code analysis",
            "$SOURCES_ROUND_COMPILER_TIME -> Compiler initialization time",
            "$SOURCES_ROUND_COMPILER_TIME -> Compiler translation to IR",
            "$SOURCES_ROUND_COMPILER_TIME -> Compiler code generation",
            "$SOURCES_ROUND_COMPILER_TIME -> Compiler code generation -> Compiler IR lowering",
            "$SOURCES_ROUND_COMPILER_TIME -> Compiler code generation -> Compiler backend",
            "$SOURCES_ROUND_COMPILER_TIME -> Compiler Klib metadata writing",
            "Total compiler iteration",
            "Total compiler iteration -> Analysis lines per second",
            "Total compiler iteration -> Code generation lines per second",
            "Total compiler iteration -> Number of lines analyzed",
        )
    }
}
