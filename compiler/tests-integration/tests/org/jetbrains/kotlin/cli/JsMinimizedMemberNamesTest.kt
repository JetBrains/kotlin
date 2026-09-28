/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.CommonJsAndWasmCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.cliArgument
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.test.CompilerTestUtil
import org.jetbrains.kotlin.test.TestCaseWithTmpdir
import org.jetbrains.kotlin.utils.PathUtil
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

private const val MODULE_NAME = "engine"

private const val SOURCE = """
@file:OptIn(ExperimentalJsExport::class)

@JsExport
class Engine {
    private var jsMinimizedMemberNamesTestCounter: Int = 0

    fun incrementCounter(): Int {
        jsMinimizedMemberNamesTestCounter += 1
        return jsMinimizedMemberNamesTestCounter
    }
}

@JsExport
fun runEngine(): Int = Engine().incrementCounter()
"""

// KT-89449
class JsMinimizedMemberNamesTest : TestCaseWithTmpdir() {

    @Test
    fun testNonExportedMembersAreMinimized() {
        val stdlib = PathUtil.kotlinPathsForCompiler.jsStdLibKlibPath.absolutePath
        val klibDir = tmpdir.resolve("klib")
        val jsDir = tmpdir.resolve("js")
        val source = tmpdir.resolve("engine.kt").apply { writeText(SOURCE) }

        compile(
            K2JSCompilerArguments::libraries.cliArgument, stdlib,
            K2JSCompilerArguments::outputDir.cliArgument(klibDir.absolutePath),
            K2JSCompilerArguments::moduleName.cliArgument(MODULE_NAME),
            source.absolutePath,
        )

        compile(
            K2JSCompilerArguments::libraries.cliArgument, stdlib,
            CommonJsAndWasmCompilerArguments::includes.cliArgument(klibDir.resolve("$MODULE_NAME.klib").absolutePath),
            K2JSCompilerArguments::outputDir.cliArgument(jsDir.absolutePath),
            K2JSCompilerArguments::moduleName.cliArgument(MODULE_NAME),
            CommonJsAndWasmCompilerArguments::irProduceJs.cliArgument,
            CommonJsAndWasmCompilerArguments::irDce.cliArgument("true"),
            K2JSCompilerArguments::irMinimizedMemberNames.cliArgument("true"),
        )

        val generated = jsDir.resolve("$MODULE_NAME.js").readText()
        assertFalse(
            "jsMinimizedMemberNamesTestCounter" in generated,
            "-Xir-minimized-member-names had no effect: non-exported property kept its Kotlin name " +
                    "'jsMinimizedMemberNamesTestCounter' in the generated JS",
        )
    }

    private fun compile(vararg args: String) {
        val result = CompilerTestUtil.executeCompiler(K2JSCompiler(), args.toList())
        assertEquals(ExitCode.OK, result.second, result.first)
    }
}
