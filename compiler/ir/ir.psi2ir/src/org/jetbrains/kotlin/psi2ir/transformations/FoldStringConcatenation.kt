/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.psi2ir.transformations

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.detach
import org.jetbrains.kotlin.ir.expressions.IrCallExpression
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStringConcatenationExpression
import org.jetbrains.kotlin.ir.expressions.IrStringConcatenationExpressionImpl
import org.jetbrains.kotlin.ir.replaceWith
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.util.OperatorNameConventions
import java.util.*

fun foldStringConcatenation(element: IrElement) {
    element.accept(FoldStringConcatenation(), null)
}

class FoldStringConcatenation : IrElementVisitor<Unit, Nothing?> {
    override fun visitElement(element: IrElement, data: Nothing?) {
        element.acceptChildren(this, data)
    }

    override fun visitCallExpression(expression: IrCallExpression, data: Nothing?) {
        if (!isStringPlus(expression.descriptor)) {
            visitElement(expression, data)
            return
        }

        val arguments = ArrayList<IrExpression>()
        collectStringConcatenationArguments(expression, arguments)
        val irStringConcatenation = IrStringConcatenationExpressionImpl(expression.startOffset, expression.endOffset, expression.type)
        arguments.forEach { irStringConcatenation.addArgument(it) }

        expression.replaceWith(irStringConcatenation)
    }

    private fun collectStringConcatenationArguments(expression: IrExpression, arguments: ArrayList<IrExpression>) {
        when {
            expression is IrCallExpression && isStringPlus(expression.descriptor)-> {
                collectStringConcatenationArguments(expression.dispatchReceiver!!, arguments)
                collectStringConcatenationArguments(expression.getArgument(0)!!, arguments)
            }
            expression is IrStringConcatenationExpression -> {
                arguments.addAll(expression.arguments)
                expression.arguments.forEach { it.detach() }
            }
            else -> {
                arguments.add(expression)
                expression.detach()
            }
        }
    }

    private fun isStringPlus(descriptor: CallableDescriptor): Boolean {
        if (descriptor.name != OperatorNameConventions.PLUS) return false
        val dispatchReceiverType = descriptor.dispatchReceiverParameter?.type ?: return false
        if (!KotlinBuiltIns.isString(dispatchReceiverType)) return false
        return true
    }
}
