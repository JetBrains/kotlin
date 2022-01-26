/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.optimization

import com.google.javascript.jscomp.*
import org.jetbrains.kotlin.ir.backend.js.CompilationOutputs
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext

fun CompilationOutputs.optimize(context: JsIrBackendContext): CompilationOutputs {
    return CompilationOutputs(
        JsOptimizer(jsCode, context).optimize(),
        jsProgram,
        sourceMap,
        dependencies
    )
}

private class JsOptimizer(output: String, context: JsIrBackendContext) {
    private val input = output.asSourceCode()
    private val extern = context.generateExternsFromExternals()
    private val closureCompiler = Compiler()
    private val closureCompilerOptions = getCompilerOptions()

    fun optimize(): String {
        return closureCompiler
            .apply { compile(extern, listOf(input), closureCompilerOptions) }
            .toSource()
    }

    private fun String.asSourceCode(): SourceFile {
        return SourceFile.fromCode("input.js", this)
    }

    private fun JsIrBackendContext.generateExternsFromExternals(): List<SourceFile> {
        val builtins = AbstractCommandLineRunner.getBuiltinExterns(CompilerOptions.Environment.CUSTOM)
        val userLandExterns = emptyList<SourceFile>()
        return builtins + userLandExterns
    }

    private fun getCompilerOptions(): CompilerOptions {
        return CompilerOptions()
            .also { CompilationLevel.ADVANCED_OPTIMIZATIONS.setOptionsForCompilationLevel(it) }
            .apply {
                isPrettyPrint = true
                environment = CompilerOptions.Environment.CUSTOM
                languageIn = CompilerOptions.LanguageMode.ECMASCRIPT5_STRICT
                assumeGettersArePure = true
                assumeStaticInheritanceIsNotUsed = true

                setSummaryDetailLevel(0)
                setAssumeStrictThis(true)
                setAssumeClosuresOnlyCaptureReferences(true)
                setWarningLevel(DiagnosticGroups.GLOBAL_THIS, CheckLevel.OFF)
                setExtractPrototypeMemberDeclarations(CompilerOptions.ExtractPrototypeMemberDeclarationsMode.USE_CHUNK_TEMP)
            }
    }
}
