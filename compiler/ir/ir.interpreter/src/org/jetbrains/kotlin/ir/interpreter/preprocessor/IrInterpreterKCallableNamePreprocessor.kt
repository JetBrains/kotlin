/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.preprocessor

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrCallableReference
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.interpreter.checker.IrInterpreterNameChecker.Companion.isKCallableNameCall
import org.jetbrains.kotlin.name.SpecialNames

// Note: this class still will not allow us to evaluate things like `A()::a.name + `A()::b.name`.
// This code will be optimized but not completely turned into "ab" result.
class IrInterpreterKCallableNamePreprocessor : IrInterpreterPreprocessor {
    override fun visitCall(expression: IrCall, data: IrInterpreterPreprocessorData): IrElement {
        if (!data.mode.canEvaluateFunction(expression.symbol.owner)) return super.visitCall(expression, data)
        if (!expression.isKCallableNameCall(data.irBuiltIns)) return super.visitCall(expression, data)

        val callableReference = expression.dispatchReceiver as? IrCallableReference<*> ?: return super.visitCall(expression, data)

        // receiver is needed for bound callable reference
        val receiver = callableReference.dispatchReceiver ?: callableReference.extensionReceiver ?: return expression

        callableReference.dispatchReceiver = null
        callableReference.extensionReceiver = null
        if (receiver is IrGetValue && receiver.symbol.owner.name == SpecialNames.THIS) return expression

        return IrCompositeImpl(
            expression.startOffset, expression.endOffset, expression.type, origin = null, statements = listOf(receiver, expression)
        )
    }
}