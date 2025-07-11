/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.backend.jvm.ir.fileParent
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.org.objectweb.asm.Label
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * This class is responsible for generating line numbers for given `IrElement`
 */
class LineNumberMapper(private val expressionCodegen: ExpressionCodegen) {
    private val irFunction = expressionCodegen.irFunction
    private val fileEntry = irFunction.fileParent.fileEntry

    private var lastLineNumber: Int = -1
    private var noLineNumberScope: Boolean = false

    private fun markNewLabel() = Label().apply { expressionCodegen.mv.visitLabel(this) }

    fun markLineNumber(element: IrElement, startOffset: Boolean) {
        if (noLineNumberScope) return
        val offset = if (startOffset) element.startOffset else element.endOffset
        if (offset < 0) return

        val lineNumber = fileEntry.getLineNumber(offset) + 1

        assert(lineNumber > 0)
        if (lastLineNumber != lineNumber) {
            lastLineNumber = lineNumber
            expressionCodegen.mv.visitLineNumber(lineNumber, markNewLabel())
        }
    }

    @OptIn(ExperimentalContracts::class)
    internal inline fun noLineNumberScopeWithCondition(flag: Boolean, block: () -> Unit) {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
        val previousState = noLineNumberScope
        noLineNumberScope = noLineNumberScope || flag
        block()
        noLineNumberScope = previousState
    }

    fun noLineNumberScope(block: () -> Unit) {
        val previousState = noLineNumberScope
        noLineNumberScope = true
        block()
        noLineNumberScope = previousState
    }

    fun markLineNumberAfterInlineIfNeeded(registerLineNumberAfterwards: Boolean) {
        if (noLineNumberScope || registerLineNumberAfterwards) {
            if (lastLineNumber > -1) {
                val label = Label()
                expressionCodegen.mv.visitLabel(label)
                expressionCodegen.mv.visitLineNumber(lastLineNumber, label)
            }
        } else {
            // Inline function has its own line number which is in a separate instance of codegen,
            // therefore we need to reset lastLineNumber to force a line number generation after visiting inline function.
            lastLineNumber = -1
        }
    }

    fun getLineNumber(): Int {
        return lastLineNumber
    }
}