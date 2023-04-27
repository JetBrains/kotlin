/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.checker

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

class IrCompileTimeNameChecker(
    private val mode: EvaluationMode
) : IrElementVisitor<Boolean, Nothing?> {
    private fun IrCall.isIntrinsicConstEvaluationNameProperty(): Boolean {
        val owner = this.symbol.owner
        if (owner.extensionReceiverParameter != null || owner.valueParameters.isNotEmpty()) return false
        val property = (owner as? IrSimpleFunction)?.correspondingPropertySymbol?.owner ?: return false
        return mode.canEvaluateFunction(owner) && property.name.asString() == "name"
    }

    override fun visitElement(element: IrElement, data: Nothing?) = false

    override fun visitCall(expression: IrCall, data: Nothing?): Boolean {
        if (!expression.isIntrinsicConstEvaluationNameProperty()) return false
        return when (val receiver = expression.dispatchReceiver) {
            is IrCallableReference<*> -> (receiver.dispatchReceiver == null || receiver.dispatchReceiver is IrGetObjectValue) && receiver.extensionReceiver == null
            is IrGetEnumValue -> true
            else -> false
        }
    }

    override fun visitStringConcatenation(expression: IrStringConcatenation, data: Nothing?): Boolean {
        val possibleNameCall = expression.arguments.singleOrNull() as? IrCall ?: return false
        return possibleNameCall.accept(this, data)
    }
}
