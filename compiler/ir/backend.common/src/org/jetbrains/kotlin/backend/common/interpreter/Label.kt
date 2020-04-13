/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.interpreter

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrReturnableBlock
import org.jetbrains.kotlin.ir.expressions.IrWhen
import org.jetbrains.kotlin.ir.expressions.IrWhileLoop

enum class ReturnLabel {
    NEXT, RETURN, BREAK_LOOP, BREAK_WHEN, CONTINUE, EXCEPTION
}

interface ExecutionResult {
    val returnLabel: ReturnLabel

    suspend fun getNextLabel(irElement: IrElement, interpret: suspend IrElement.() -> ExecutionResult): ExecutionResult
}

inline fun ExecutionResult.check(toCheckLabel: ReturnLabel = ReturnLabel.NEXT, returnBlock: (ExecutionResult) -> Unit): ExecutionResult {
    if (this.returnLabel != toCheckLabel) returnBlock(this)
    return this
}

open class ExecutionResultWithoutInfo(override val returnLabel: ReturnLabel) : ExecutionResult {
    override suspend fun getNextLabel(irElement: IrElement, interpret: suspend IrElement.() -> ExecutionResult): ExecutionResult {
        return when (returnLabel) {
            ReturnLabel.RETURN -> this
            ReturnLabel.BREAK_WHEN -> when (irElement) {
                is IrWhen -> Next
                else -> this
            }
            ReturnLabel.BREAK_LOOP -> this
            ReturnLabel.CONTINUE -> this
            ReturnLabel.EXCEPTION -> this
            ReturnLabel.NEXT -> this
        }
    }

    fun addInfo(info: String): ExecutionResultWithInfo {
        return ExecutionResultWithInfo(returnLabel, info)
    }
}

class ExecutionResultWithInfo(override val returnLabel: ReturnLabel, val info: String) : ExecutionResultWithoutInfo(returnLabel) {
    override suspend fun getNextLabel(irElement: IrElement, interpret: suspend IrElement.() -> ExecutionResult): ExecutionResult {
        return when (returnLabel) {
            ReturnLabel.RETURN -> when (irElement) {
                is IrCall -> if (info == irElement.symbol.descriptor.toString()) Next else this
                is IrReturnableBlock -> if (info == irElement.symbol.descriptor.toString()) Next else this
                is IrFunctionImpl -> if (info == irElement.descriptor.toString()) Next else this
                else -> this
            }
            ReturnLabel.BREAK_WHEN -> when (irElement) {
                is IrWhen -> Next
                else -> this
            }
            ReturnLabel.BREAK_LOOP -> when (irElement) {
                is IrWhileLoop -> if ((irElement.label ?: "") == info) Next else this
                else -> this
            }
            ReturnLabel.CONTINUE -> when (irElement) {
                is IrWhileLoop -> if ((irElement.label ?: "") == info) irElement.interpret() else this
                else -> this
            }
            ReturnLabel.EXCEPTION -> Exception
            ReturnLabel.NEXT -> Next
        }
    }
}

object Next : ExecutionResultWithoutInfo(ReturnLabel.NEXT)
object Return : ExecutionResultWithoutInfo(ReturnLabel.RETURN)
object BreakLoop : ExecutionResultWithoutInfo(ReturnLabel.BREAK_LOOP)
object BreakWhen : ExecutionResultWithoutInfo(ReturnLabel.BREAK_WHEN)
object Continue : ExecutionResultWithoutInfo(ReturnLabel.CONTINUE)
object Exception : ExecutionResultWithoutInfo(ReturnLabel.EXCEPTION)