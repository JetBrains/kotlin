/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.integration

import org.jetbrains.kotlin.test.util.KtTestUtil
import java.io.File

class ParallelBuildTest : KotlinIntegrationTestBase() {
    fun testParallelBuild() {
        fun rawString(text: String): String = "\"\"\"$text\"\"\""

        val testDataDir = KtTestUtil.getTestDataPathBase() + "/integration/smoke/helloApp"
        val program = ProgramWithDependencyOnCompiler(
            tmpdir, """
            import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
            import org.jetbrains.kotlin.cli.common.ExitCode
            import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
            import java.util.concurrent.Executors
            import java.util.concurrent.TimeUnit

            // Increase `totalRuns` and change `threads` to `Runtime.getRuntime().availableProcessors()` to reproduce more reliably locally
            val totalRuns = 100
            val threads = 2

            fun main() {
                var errors = 0 

                val pool = Executors.newFixedThreadPool(threads)
                repeat(totalRuns) {
                    pool.submit {
                        try {
                            val code = K2JVMCompiler().exec(
                                System.err, MessageRenderer.PLAIN_RELATIVE_PATHS,
                                ${rawString("$testDataDir/hello.kt")},
                                "-d", ${rawString("${tmpdir.path}/output")}
                            )
                            if (code != ExitCode.OK) errors++
                        } catch (e: Throwable) {
                            errors++
                            throw e
                        }
                    }
                }
                pool.shutdown()
                pool.awaitTermination(120L, TimeUnit.SECONDS)

                println("" + errors + " errors")
                if (errors > 0) {
                    System.exit(1)
                }
            }
            """.trimIndent()
        )

        program.compile()

        val result = program.run(File(testDataDir))
        assertEquals("0 errors", result)
    }
}
