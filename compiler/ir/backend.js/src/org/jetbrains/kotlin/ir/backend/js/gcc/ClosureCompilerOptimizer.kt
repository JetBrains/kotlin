/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.gcc

import com.google.javascript.jscomp.*
import org.jetbrains.kotlin.backend.common.compilationException
import org.jetbrains.kotlin.ir.IrFileEntry
import org.jetbrains.kotlin.ir.SourceRangeInfo
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.CompilationOutputs
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.IrDeclarationToJsTransformer
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.JsNameLinkingNamer
import org.jetbrains.kotlin.ir.backend.js.utils.JsGenerationContext
import org.jetbrains.kotlin.ir.backend.js.utils.JsStaticContext
import org.jetbrains.kotlin.ir.backend.js.utils.NameTable
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrFileSymbolImpl
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.js.backend.JsToStringGenerationVisitor
import org.jetbrains.kotlin.js.backend.ast.JsGlobalBlock
import org.jetbrains.kotlin.js.util.TextOutputImpl
import org.jetbrains.kotlin.name.FqName

fun CompilationOutputs.optimize(context: JsIrBackendContext): CompilationOutputs {
    return CompilationOutputs(
        ClosureCompilerOptimizer(jsCode, context).optimize(), jsProgram, sourceMap, dependencies
    )
}

private class ClosureCompilerOptimizer(output: String, context: JsIrBackendContext) {
    private val declarationTransformer = IrDeclarationToJsTransformer()
    private val input = output.asSourceCode()
    private val extern = context.generateExternsFromExternals()
    private val closureCompiler = Compiler()
    private val closureCompilerOptions = getCompilerOptions()

    fun optimize(): String {
        return closureCompiler.apply {
            val result = compile(extern, listOf(input), closureCompilerOptions)
            if (!result.success) {
                compilationException("Something went wrong with optimization compiler", null)
            }
        }.toSource()
    }

    private fun String.asSourceCode(): SourceFile {
        return SourceFile.fromCode("input.js", this)
    }

    private fun JsIrBackendContext.generateExternsFromExternals(): List<SourceFile> {
        val builtins = AbstractCommandLineRunner.getBuiltinExterns(CompilerOptions.Environment.CUSTOM)
        val userLandExterns = generateUserLandExterns()
        return builtins + userLandExterns
    }

    private fun JsIrBackendContext.generateUserLandExterns(): SourceFile {
        val nameGenerator = JsNameLinkingNamer(this)

        val globalNameScope = NameTable<IrDeclaration>()

        val staticContext = JsStaticContext(
            backendContext = this, irNamer = nameGenerator, globalNameScope = globalNameScope
        )

        val generationContext = JsGenerationContext(
            currentFile = IrFileImpl(
                DummyFile, IrFileSymbolImpl(null), FqName(DummyFile.name)
            ), currentFunction = null, staticContext = staticContext, useBareParameterNames = false
        )
        val externs = JsGlobalBlock().apply {
            statements += externalPackageFragment.asSequence().filter {
                !it.value.module.isCommonStdLib(this@generateUserLandExterns) && !it.value.module.isJsStdLib(this@generateUserLandExterns)
            }.flatMap { it.value.declarations }.mapNotNull {
                when (it) {
                    is IrProperty -> it.backingField
                    else -> it
                }
            }.map { it.accept(declarationTransformer, generationContext) }.toList()
        }

        val output = TextOutputImpl().apply {
            print(
                """
                /**
                 * @fileoverview This is an externs file.
                 * @externs
                 */
            """.trimIndent()
            )
            print('\n')
            externs.accept(JsToStringGenerationVisitor(this))
        }
        return SourceFile.fromCode(DummyFile.name, output.toString())
    }

    private fun IrModuleFragment.isJsStdLib(context: JsIrBackendContext): Boolean {
        return context.intrinsics.jsCode.owner.file.module === this
    }

    private fun IrModuleFragment.isCommonStdLib(context: JsIrBackendContext): Boolean {
        return context.irBuiltIns.annotationClass.owner.file.module === this
    }

    private fun getCompilerOptions(): CompilerOptions {
        return CompilerOptions().also { CompilationLevel.ADVANCED_OPTIMIZATIONS.setOptionsForCompilationLevel(it) }.apply {
            isPrettyPrint = true
            environment = CompilerOptions.Environment.CUSTOM
            languageIn = CompilerOptions.LanguageMode.ECMASCRIPT5_STRICT
            assumeGettersArePure = true
            assumeStaticInheritanceIsNotUsed = true

            setSummaryDetailLevel(0)
            setAssumeStrictThis(true)
            setMaxOptimizationLoopIterations(101)
            setAssumeClosuresOnlyCaptureReferences(true)
            setWarningLevel(DiagnosticGroups.GLOBAL_THIS, CheckLevel.OFF)
            setWarningLevel(DiagnosticGroups.CHECK_TYPES, CheckLevel.OFF)
            setExtractPrototypeMemberDeclarations(CompilerOptions.ExtractPrototypeMemberDeclarationsMode.USE_CHUNK_TEMP)
        }
    }

    private object DummyFile : IrFileEntry {
        override val name = "<closure-compiler-externs>"
        override val maxOffset = UNDEFINED_OFFSET

        override fun getSourceRangeInfo(beginOffset: Int, endOffset: Int) = SourceRangeInfo(
            "", UNDEFINED_OFFSET, UNDEFINED_OFFSET, UNDEFINED_OFFSET, UNDEFINED_OFFSET, UNDEFINED_OFFSET, UNDEFINED_OFFSET
        )

        override fun getLineNumber(offset: Int) = UNDEFINED_OFFSET
        override fun getColumnNumber(offset: Int) = UNDEFINED_OFFSET
    }
}
