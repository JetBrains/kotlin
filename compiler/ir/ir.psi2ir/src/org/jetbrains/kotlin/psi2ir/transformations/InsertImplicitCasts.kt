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
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.detach
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.replaceWith
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.psi2ir.containsNull
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.isNullabilityFlexible
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import org.jetbrains.kotlin.types.upperIfFlexible

fun insertImplicitCasts(builtIns: KotlinBuiltIns, element: IrElement) {
    element.acceptVoid(InsertImplicitCasts(builtIns))
}

class InsertImplicitCasts(val builtIns: KotlinBuiltIns): IrElementVisitorVoid {
    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitGeneralCall(expression: IrGeneralCall) {
        expression.acceptChildrenVoid(this)

        with(expression) {
            dispatchReceiver?.replaceWithCast(descriptor.dispatchReceiverParameter?.type)
            extensionReceiver?.replaceWithCast(descriptor.extensionReceiverParameter?.type)
            for (index in descriptor.valueParameters.indices) {
                val parameterType = descriptor.valueParameters[index].type
                getArgument(index)?.replaceWithCast(parameterType)
            }
        }
    }

    override fun visitBlock(expression: IrBlock) {
        expression.acceptChildren(this, null)
        val type = expression.type
        if (KotlinBuiltIns.isUnit(type)) return
        if (expression.statements.isEmpty()) return

        val lastStatement = expression.statements.last()
        if (lastStatement is IrExpression) {
            lastStatement.replaceWithCast(type)
        }
    }

    override fun visitReturn(expression: IrReturn) {
        expression.acceptChildren(this, null)

        expression.value?.replaceWithCast(expression.returnTarget.returnType)
    }

    override fun visitSetVariable(expression: IrSetVariable) {
        expression.acceptChildren(this, null)

        expression.value.replaceWithCast(expression.descriptor.type)
    }

    override fun visitVariable(declaration: IrVariable) {
        declaration.acceptChildren(this, null)

        declaration.initializer?.replaceWithCast(declaration.descriptor.type)
    }

    override fun visitWhen(expression: IrWhen) {
        expression.acceptChildren(this, null)

        for (branchIndex in expression.branchIndices) {
            expression.getNthCondition(branchIndex)!!.replaceWithCast(builtIns.booleanType)
            expression.getNthResult(branchIndex)!!.replaceWithCast(expression.type)
        }
        expression.elseBranch?.replaceWithCast(expression.type)
    }

    override fun visitLoop(loop: IrLoop) {
        loop.acceptChildren(this, null)

        loop.condition.replaceWithCast(builtIns.booleanType)
    }

    override fun visitThrow(expression: IrThrow) {
        expression.acceptChildren(this, null)

        expression.value.replaceWithCast(builtIns.throwable.defaultType)
    }

    override fun visitTryCatch(tryCatch: IrTryCatch) {
        tryCatch.acceptChildren(this, null)

        val resultType = tryCatch.type

        tryCatch.tryResult.replaceWithCast(resultType)
        for (catchClauseIndex in tryCatch.catchClauseIndices) {
            tryCatch.getNthCatchResult(catchClauseIndex)!!.replaceWithCast(resultType)
        }
    }

    override fun visitVararg(expression: IrVararg) {
        expression.acceptChildrenVoid(this)
        for (element in expression.elements) {
            when (element) {
                is IrSpreadElement -> element.expression.replaceWithCast(expression.type)
                is IrExpression -> element.replaceWithCast(expression.varargElementType)
            }
        }
    }

    private fun IrExpression.replaceWithCast(expectedType: KotlinType?) {
        replaceWith { it.wrapWithImplicitCast(expectedType) }
    }

    private fun IrExpression.wrapWithImplicitCast(expectedType: KotlinType?): IrExpression {
        if (expectedType == null) return this
        if (expectedType.isError) return this
        if (KotlinBuiltIns.isUnit(expectedType)) return this // TODO expose coercion to Unit in IR?

        val valueType = this.type

        if (valueType.isNullabilityFlexible() && valueType.containsNull() && !expectedType.containsNull()) {
            val nonNullValueType = valueType.upperIfFlexible().makeNotNullable();
            return IrTypeOperatorCallImpl(
                    this.startOffset, this.endOffset, nonNullValueType,
                    IrTypeOperator.IMPLICIT_NOTNULL, nonNullValueType, this.detach()
            ).wrapWithImplicitCast(expectedType)
        }

        if (!KotlinTypeChecker.DEFAULT.isSubtypeOf(valueType.makeNotNullable(), expectedType)) {
            return IrTypeOperatorCallImpl(this.startOffset, this.endOffset, expectedType,
                                          IrTypeOperator.IMPLICIT_CAST, expectedType, this.detach())
        }

        return this
    }
}

