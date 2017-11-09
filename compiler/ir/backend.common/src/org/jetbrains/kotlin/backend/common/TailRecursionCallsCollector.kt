/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.util.usesDefaultArguments
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.types.typeUtil.isUnit

/**
 * Collects calls to be treated as tail recursion.
 * The checks are partially based on the frontend implementation
 * in `ControlFlowInformationProvider.markAndCheckRecursiveTailCalls()`.
 *
 * This analysis is not very precise and can miss some calls.
 * It is also not guaranteed that each returned call is detected as tail recursion by the frontend.
 * However any returned call can be correctly optimized as tail recursion.
 */
fun collectTailRecursionCalls(irFunction: IrFunction): Set<IrCall> {
    if (!irFunction.descriptor.isTailrec) {
        return emptySet()
    }

    val result = mutableSetOf<IrCall>()

    val visitor = object : IrElementVisitor<Unit, ElementKind> {

        override fun visitElement(element: IrElement, data: ElementKind) {
            val childKind = ElementKind.NOT_SURE // Not sure by default.
            element.acceptChildren(this, childKind)
        }

        override fun visitFunction(declaration: IrFunction, data: ElementKind) {
            // Ignore local functions.
        }

        override fun visitClass(declaration: IrClass, data: ElementKind) {
            // Ignore local classes.
        }

        override fun visitTry(aTry: IrTry, data: ElementKind) {
            // We do not support tail calls in try-catch-finally, for simplicity of the mental model
            // very few cases there would be real tail-calls, and it's often not so easy for the user to see why
        }

        override fun visitReturn(expression: IrReturn, data: ElementKind) {
            val valueKind = if (expression.returnTarget == irFunction.descriptor) {
                ElementKind.TAIL_STATEMENT
            } else {
                ElementKind.NOT_SURE
            }
            expression.value.accept(this, valueKind)
        }

        override fun visitContainerExpression(expression: IrContainerExpression, data: ElementKind) {
            expression.statements.forEachIndexed { index, irStatement ->
                val statementKind = if (index == expression.statements.lastIndex) {
                    // The last statement defines the result of the container expression, so it has the same kind.
                    data
                } else {
                    ElementKind.NOT_SURE
                }
                irStatement.accept(this, statementKind)
            }
        }

        override fun visitWhen(expression: IrWhen, data: ElementKind) {
            expression.branches.forEach {
                it.condition.accept(this, ElementKind.NOT_SURE)
                it.result.accept(this, data)
            }
        }

        override fun visitCall(expression: IrCall, data: ElementKind) {
            expression.acceptChildren(this, ElementKind.NOT_SURE)

            // Is it a tail call?
            if (data != ElementKind.TAIL_STATEMENT) {
                return
            }

            // Is it a recursive call?
            if (expression.descriptor.original != irFunction.descriptor) {
                return
            }
            // TODO: check type arguments

            if (DescriptorUtils.isOverride(irFunction.descriptor) && expression.usesDefaultArguments()) {
                // Overridden functions using default arguments at tail call are not included: KT-4285
                return
            }

            expression.dispatchReceiver?.let {
                if (it !is IrGetValue || it.descriptor != irFunction.descriptor.dispatchReceiverParameter) {
                    // A tail call is not allowed to change dispatch receiver
                    //   class C {
                    //       fun foo(other: C) {
                    //           other.foo(this) // not a tail call
                    //       }
                    //   }
                    return
                }
            }

            result.add(expression)

        }

    }

    val body = irFunction.body
    if (body !is IrBlockBody) {
        return emptySet() // TODO: should an assert be here instead?
    }

    body.statements.forEachIndexed { index, irStatement ->
        val kind = if (index == body.statements.lastIndex && irFunction.descriptor.returnType?.isUnit() == true) {
            ElementKind.TAIL_STATEMENT
        } else {
            ElementKind.NOT_SURE
        }
        irStatement.accept(visitor, kind)
    }

    return result
}

/**
 * The kind of IR element used to detect tail calls.
 */
private enum class ElementKind {
    /**
     * This element is the last statement to be executed before the return from the function.
     * If the return type is not `Unit`, the result of this statement defines the result of the entire function.
     */
    TAIL_STATEMENT,

    /**
     * Not sure if the element meets the requirements to be [TAIL_STATEMENT].
     */
    NOT_SURE
}
