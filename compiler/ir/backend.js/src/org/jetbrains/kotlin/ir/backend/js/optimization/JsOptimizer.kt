/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.optimization

import com.google.javascript.jscomp.*
import org.jetbrains.kotlin.ir.backend.js.CompilationOutputs

fun CompilationOutputs.optimize(): CompilationOutputs {
    return CompilationOutputs(
        JsOptimizer(jsCode).optimize(),
        jsProgram,
        sourceMap,
        dependencies
    )
}

private class JsOptimizer(output: String) {
    private val input = SourceFile.fromCode("input.js", output)
    private val closureCompiler = Compiler()
    private val extern = SourceFile.fromCode("externs.js", "")
    private val closureCompilerOptions = CompilerOptions()
        .also { CompilationLevel.ADVANCED_OPTIMIZATIONS.setOptionsForCompilationLevel(it) }
        .apply {
            isPrettyPrint = true
            environment = CompilerOptions.Environment.BROWSER
            languageIn = CompilerOptions.LanguageMode.ECMASCRIPT5_STRICT

            setSummaryDetailLevel(0)
            setWarningLevel(DiagnosticGroups.GLOBAL_THIS, CheckLevel.OFF)
        }

    fun optimize(): String {
        return closureCompiler
            .apply { compile(extern, input, closureCompilerOptions) }
            .toSource()
    }
}
