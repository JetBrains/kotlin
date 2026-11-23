/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.integration

import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Test
import java.io.File

class CompilationsInOneProcessTest : KotlinIntegrationTestBase() {
    // KT-91871
    @Test
    fun metadataDoesNotDependOnEarlierCompilationInProcess() {
        val testDataDir = ForTestCompileRuntime.transformTestDataPath("compiler/tests-integration/testData/integration/typeAnnotationOrder")
        fun source(name: String) = File(testDataDir, name).absolutePath

        val program = ProgramWithDependencyOnCompiler(
            tmpdir, """
            import org.jetbrains.kotlin.cli.common.ExitCode
            import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler

            fun main(args: Array<String>) {
                val compilations = mutableListOf(mutableListOf<String>())
                for (arg in args) {
                    if (arg == "--") compilations.add(mutableListOf()) else compilations.last().add(arg)
                }
                for (compilation in compilations) {
                    check(K2JVMCompiler().exec(System.err, *compilation.toTypedArray()) == ExitCode.OK) { "Compilation failed" }
                }
            }
            """.trimIndent()
        )
        program.compile()

        val sample = listOf(source("NotNull.java"), source("JavaApi.java"), source("Target.kt"))
        val aloneOutput = File(tmpdir, "alone")
        val firstOutput = File(tmpdir, "first")
        val afterFirstOutput = File(tmpdir, "afterFirst")

        program.run(testDataDir, *sample.toTypedArray(), "-d", aloneOutput.path)
        program.run(
            testDataDir,
            source("First.kt"), "-d", firstOutput.path,
            "--",
            *sample.toTypedArray(), "-d", afterFirstOutput.path,
        )

        assertArrayEquals(
            File(aloneOutput, "Target.class").readBytes(),
            File(afterFirstOutput, "Target.class").readBytes(),
            "Target.class compiled after another compilation in the same process differs from Target.class compiled alone " +
                    "(KT-91871: the order of type annotations in kotlin.Metadata depends on the earlier compilation)",
        )
    }
}
