/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.interpreter

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.lazy.IrLazyFunction
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

open class ExecutionResultWithoutInfoAboutOwner(override val returnLabel: ReturnLabel) : ExecutionResult {
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

    fun addOwnerInfo(owner: IrElement): ExecutionResultWithInfoAboutOwner {
        return ExecutionResultWithInfoAboutOwner(returnLabel, owner)
    }
}

class ExecutionResultWithInfoAboutOwner(
    override val returnLabel: ReturnLabel, private val owner: IrElement
) : ExecutionResultWithoutInfoAboutOwner(returnLabel) {
    override suspend fun getNextLabel(irElement: IrElement, interpret: suspend IrElement.() -> ExecutionResult): ExecutionResult {
        return when (returnLabel) {
            ReturnLabel.RETURN -> when (irElement) {
                is IrCall, is IrReturnableBlock, is IrFunctionImpl, is IrLazyFunction -> if (owner == irElement) Next else this
                else -> this
            }
            ReturnLabel.BREAK_WHEN -> when (irElement) {
                is IrWhen -> Next
                else -> this
            }
            ReturnLabel.BREAK_LOOP -> when (irElement) {
                is IrWhileLoop -> if (owner == irElement) Next else this
                else -> this
            }
            ReturnLabel.CONTINUE -> when (irElement) {
                is IrWhileLoop -> if (owner == irElement) irElement.interpret() else this
                else -> this
            }
            ReturnLabel.EXCEPTION -> Exception
            ReturnLabel.NEXT -> Next
        }
    }
}

object Next : ExecutionResultWithoutInfoAboutOwner(ReturnLabel.NEXT)
object Return : ExecutionResultWithoutInfoAboutOwner(ReturnLabel.RETURN)
object BreakLoop : ExecutionResultWithoutInfoAboutOwner(ReturnLabel.BREAK_LOOP)
object BreakWhen : ExecutionResultWithoutInfoAboutOwner(ReturnLabel.BREAK_WHEN)
object Continue : ExecutionResultWithoutInfoAboutOwner(ReturnLabel.CONTINUE)
object Exception : ExecutionResultWithoutInfoAboutOwner(ReturnLabel.EXCEPTION)