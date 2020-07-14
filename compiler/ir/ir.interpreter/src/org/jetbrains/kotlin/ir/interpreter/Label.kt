/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter

import org.jetbrains.kotlin.ir.interpreter.stack.Stack
import org.jetbrains.kotlin.ir.interpreter.state.Primitive
import org.jetbrains.kotlin.ir.interpreter.state.isSubtypeOf
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrReturnableBlock
import org.jetbrains.kotlin.ir.expressions.IrWhen
import org.jetbrains.kotlin.ir.expressions.IrWhileLoop
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.render

enum class ReturnLabel {
    REGULAR, RETURN, BREAK_LOOP, BREAK_WHEN, CONTINUE, EXCEPTION
}

open class ExecutionResult(val returnLabel: ReturnLabel, private val owner: IrElement? = null) {
    fun getNextLabel(irElement: IrElement, interpret: IrElement.() -> ExecutionResult): ExecutionResult {
        return when (returnLabel) {
            ReturnLabel.RETURN -> when (irElement) {
                is IrCall, is IrReturnableBlock, is IrSimpleFunction -> if (owner == irElement) Next else this
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
            ReturnLabel.REGULAR -> Next
        }
    }

    fun addOwnerInfo(owner: IrElement): ExecutionResult {
        return ExecutionResult(returnLabel, owner)
    }
}

inline fun ExecutionResult.check(toCheckLabel: ReturnLabel = ReturnLabel.REGULAR, returnBlock: (ExecutionResult) -> Unit): ExecutionResult {
    if (this.returnLabel != toCheckLabel) returnBlock(this)
    return this
}

/**
 * This method is analog of `checkcast` jvm bytecode operation. Throw exception whenever actual type is not a subtype of expected.
 */
internal fun ExecutionResult.implicitCastIfNeeded(expectedType: IrType, actualType: IrType, stack: Stack): ExecutionResult {
    if (actualType.classifierOrNull !is IrTypeParameterSymbol) return this

    if (expectedType.classifierOrFail is IrTypeParameterSymbol) return this

    val actualState = stack.peekReturnValue()
    if (actualState is Primitive<*> && actualState.value == null) return this // this is handled as NullPointerException

    if (!actualState.isSubtypeOf(expectedType)) {
        val convertibleClassName = stack.popReturnValue().irClass.fqNameWhenAvailable
        throw ClassCastException("$convertibleClassName cannot be cast to ${expectedType.render()}")
    }
    return this
}

object Next : ExecutionResult(ReturnLabel.REGULAR)
object Return : ExecutionResult(ReturnLabel.RETURN)
object BreakLoop : ExecutionResult(ReturnLabel.BREAK_LOOP)
object BreakWhen : ExecutionResult(ReturnLabel.BREAK_WHEN)
object Continue : ExecutionResult(ReturnLabel.CONTINUE)
object Exception : ExecutionResult(ReturnLabel.EXCEPTION)