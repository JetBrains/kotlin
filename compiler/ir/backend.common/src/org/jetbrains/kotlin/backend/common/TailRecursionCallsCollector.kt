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

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.isClassWithFqName
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.usesDefaultArguments
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

data class TailCalls(val ir: Set<IrCall>, val fromManyFunctions: Boolean)

/**
 * Collects calls to be treated as tail recursion.
 * The checks are partially based on the frontend implementation
 * in `ControlFlowInformationProvider.markAndCheckRecursiveTailCalls()`.
 *
 * This analysis is not very precise and can miss some calls.
 * It is also not guaranteed that each returned call is detected as tail recursion by the frontend.
 * However any returned call can be correctly optimized as tail recursion.
 */
fun collectTailRecursionCalls(irFunction: IrFunction, followFunctionReference: (IrFunctionReference) -> Boolean): TailCalls {
    if ((irFunction as? IrSimpleFunction)?.isTailrec != true) {
        return TailCalls(emptySet(), false)
    }

    class VisitorState(val isTailExpression: Boolean, val inOtherFunction: Boolean)

    val isUnitReturn = irFunction.returnType.isUnit()
    val result = mutableSetOf<IrCall>()
    var someCallsAreInOtherFunctions = false
    val visitor = object : IrElementVisitor<Unit, VisitorState> {
        override fun visitElement(element: IrElement, data: VisitorState) {
            element.acceptChildren(this, VisitorState(isTailExpression = false, data.inOtherFunction))
        }

        override fun visitFunction(declaration: IrFunction, data: VisitorState) {
            // Ignore local functions.
        }

        override fun visitClass(declaration: IrClass, data: VisitorState) {
            // Ignore local classes.
        }

        override fun visitTry(aTry: IrTry, data: VisitorState) {
            // We do not support tail calls in try-catch-finally, for simplicity of the mental model
            // very few cases there would be real tail-calls, and it's often not so easy for the user to see why
        }

        override fun visitReturn(expression: IrReturn, data: VisitorState) {
            expression.value.accept(this, VisitorState(expression.returnTargetSymbol == irFunction.symbol, data.inOtherFunction))
        }

        override fun visitExpressionBody(body: IrExpressionBody, data: VisitorState) =
            body.acceptChildren(this, data)

        override fun visitBlockBody(body: IrBlockBody, data: VisitorState) =
            visitStatementContainer(body, data)

        override fun visitContainerExpression(expression: IrContainerExpression, data: VisitorState) =
            visitStatementContainer(expression, data)

        private fun visitStatementContainer(expression: IrStatementContainer, data: VisitorState) {
            expression.statements.forEachIndexed { index, irStatement ->
                val isTailStatement = if (index == expression.statements.lastIndex) {
                    // The last statement defines the result of the container expression, so it has the same kind.
                    data.isTailExpression
                } else {
                    // In a Unit-returning function, any statement directly followed by a `return` is a tail statement.
                    isUnitReturn && expression.statements[index + 1].let {
                        it is IrReturn && it.returnTargetSymbol == irFunction.symbol && it.value.isUnitRead()
                    }
                }
                irStatement.accept(this, VisitorState(isTailStatement, data.inOtherFunction))
            }
        }

        private fun IrExpression.isUnitRead(): Boolean =
            this is IrGetObjectValue && symbol.isClassWithFqName(StandardNames.FqNames.unit)

        override fun visitWhen(expression: IrWhen, data: VisitorState) {
            expression.branches.forEach {
                it.condition.accept(this, VisitorState(isTailExpression = false, data.inOtherFunction))
                it.result.accept(this, data)
            }
        }

        override fun visitCall(expression: IrCall, data: VisitorState) {
            expression.acceptChildren(this, VisitorState(isTailExpression = false, data.inOtherFunction))

            // TODO: the frontend generates diagnostics on calls that are not optimized. This may or may not
            //   match what the backend does here. It'd be great to validate that the two are in agreement.
            if (!data.isTailExpression || expression.symbol != irFunction.symbol) {
                return
            }
            // TODO: check type arguments

            if (irFunction.overriddenSymbols.isNotEmpty() && expression.usesDefaultArguments()) {
                // Overridden functions using default arguments at tail call are not included: KT-4285
                return
            }

            val hasSameDispatchReceiver =
                irFunction.dispatchReceiverParameter?.type?.classOrNull?.owner?.kind?.isSingleton == true ||
                        expression.dispatchReceiver?.let { it is IrGetValue && it.symbol.owner == irFunction.dispatchReceiverParameter } != false
            if (!hasSameDispatchReceiver) {
                // A tail call is not allowed to change dispatch receiver
                //   class C {
                //       fun foo(other: C) {
                //           other.foo(this) // not a tail call
                //       }
                //   }
                // TODO: KT-15341 - if the tailrec function is neither `override` nor `open`, this is fine actually?
                //   Probably requires editing the frontend too.
                return
            }

            if (data.inOtherFunction) {
                someCallsAreInOtherFunctions = true
            }
            result.add(expression)
        }

        override fun visitFunctionReference(expression: IrFunctionReference, data: VisitorState) {
            expression.acceptChildren(this, VisitorState(isTailExpression = false, data.inOtherFunction))
            // This should match inline lambdas:
            //   tailrec fun foo() {
            //     run { return foo() } // non-local return from `foo`, so this *is* a tail call
            //   }
            // Whether crossinline lambdas are matched is unimportant, as they can't contain any returns
            // from `foo` anyway.
            if (followFunctionReference(expression)) {
                // If control reaches end of lambda, it will *not* end the current function by default,
                // so the lambda's body itself is not a tail statement.
                expression.symbol.owner.body?.accept(this, VisitorState(isTailExpression = false, inOtherFunction = true))
            }
        }
    }

    irFunction.body?.accept(visitor, VisitorState(isTailExpression = true, inOtherFunction = false))
    return TailCalls(result, someCallsAreInOtherFunctions)
}
