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
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.psi2ir.containsNull
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.isNullabilityFlexible
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import org.jetbrains.kotlin.types.upperIfFlexible

fun insertImplicitCasts(builtIns: KotlinBuiltIns, element: IrElement) {
    element.transformChildren(InsertImplicitCasts(builtIns), null)
}

class InsertImplicitCasts(val builtIns: KotlinBuiltIns): IrElementTransformer<Nothing?> {
    override fun visitElement(element: IrElement, data: Nothing?): IrElement {
        element.transformChildren(this, data)
        return element
    }

    override fun visitGeneralCall(expression: IrGeneralCall, data: Nothing?): IrExpression {
        expression.transformChildren(this, data)

        with(expression) {
            dispatchReceiver = dispatchReceiver?.wrapWithImplicitCast(descriptor.dispatchReceiverParameter?.type)
            extensionReceiver = extensionReceiver?.wrapWithImplicitCast(descriptor.extensionReceiverParameter?.type)
            for (index in descriptor.valueParameters.indices) {
                val argument = getArgument(index) ?: continue
                val parameterType = descriptor.valueParameters[index].type
                putArgument(index, argument.wrapWithImplicitCast(parameterType))
            }
        }

        return expression
    }

    override fun visitBlock(expression: IrBlock, data: Nothing?): IrExpression {
        expression.transformChildren(this, data)

        val type = expression.type
        if (expression.statements.isEmpty() || KotlinBuiltIns.isUnit(type) || KotlinBuiltIns.isNothing(type)) {
            return expression
        }

        val lastStatement = expression.statements.last()
        if (lastStatement is IrExpression) {
            expression.putStatement(expression.statements.lastIndex, lastStatement.wrapWithImplicitCast(type))
        }

        return expression
    }

    override fun visitReturn(expression: IrReturn, data: Nothing?): IrExpression {
        expression.transformChildren(this, data)

        expression.value = expression.value?.wrapWithImplicitCast(expression.returnTarget.returnType)

        return expression
    }

    override fun visitSetVariable(expression: IrSetVariable, data: Nothing?): IrExpression {
        expression.transformChildren(this, data)

        expression.value = expression.value.wrapWithImplicitCast(expression.descriptor.type)

        return expression
    }

    override fun visitSetField(expression: IrSetField, data: Nothing?): IrExpression {
        expression.transformChildren(this, data)

        expression.value = expression.value.wrapWithImplicitCast(expression.descriptor.type)

        return expression
    }

    override fun visitVariable(declaration: IrVariable, data: Nothing?): IrVariable {
        declaration.transformChildren(this, data)

        declaration.initializer = declaration.initializer?.wrapWithImplicitCast(declaration.descriptor.type)

        return declaration
    }

    override fun visitWhen(expression: IrWhen, data: Nothing?): IrExpression {
        expression.transformChildren(this, data)

        val resultType = expression.type

        for (i in expression.branchIndices) {
            val nthCondition = expression.getNthCondition(i)!!
            val nthResult = expression.getNthResult(i)!!

            expression.putNthCondition(i, nthCondition.wrapWithImplicitCast(builtIns.booleanType))
            expression.putNthResult(i, nthResult.wrapWithImplicitCast(resultType))
        }

        expression.elseBranch = expression.elseBranch?.wrapWithImplicitCast(resultType)

        return expression
    }

    override fun visitLoop(loop: IrLoop, data: Nothing?): IrExpression {
        loop.transformChildren(this, data)

        loop.condition = loop.condition.wrapWithImplicitCast(builtIns.booleanType)

        return loop
    }

    override fun visitThrow(expression: IrThrow, data: Nothing?): IrExpression {
        expression.transformChildren(this, data)

        expression.value = expression.value.wrapWithImplicitCast(builtIns.throwable.defaultType)

        return expression
    }

    override fun visitTryCatch(tryCatch: IrTryCatch, data: Nothing?): IrExpression {
        tryCatch.transformChildren(this, data)

        val resultType = tryCatch.type

        tryCatch.tryResult = tryCatch.tryResult.wrapWithImplicitCast(resultType)

        for (i in tryCatch.catchClauseIndices) {
            val nthCatchResult = tryCatch.getNthCatchResult(i)!!
            tryCatch.putNthCatchResult(i, nthCatchResult.wrapWithImplicitCast(resultType))
        }

        return tryCatch
    }

    override fun visitVararg(expression: IrVararg, data: Nothing?): IrExpression {
        expression.transformChildren(this, data)

        expression.elements.forEachIndexed { i, element ->
            when (element) {
                is IrSpreadElement ->
                    element.expression = element.expression.wrapWithImplicitCast(expression.type)
                is IrExpression ->
                    expression.putElement(i, element.wrapWithImplicitCast(expression.varargElementType))
            }
        }

        return expression
    }

    private fun IrExpression.wrapWithImplicitCast(expectedType: KotlinType?): IrExpression {
        if (expectedType == null) return this
        if (expectedType.isError) return this
        if (KotlinBuiltIns.isUnit(expectedType)) return this // TODO expose coercion to Unit in IR?

        val valueType = this.type

        if (valueType.isNullabilityFlexible() && valueType.containsNull() && !expectedType.containsNull()) {
            val nonNullValueType = valueType.upperIfFlexible().makeNotNullable()
            return IrTypeOperatorCallImpl(
                    this.startOffset, this.endOffset, nonNullValueType,
                    IrTypeOperator.IMPLICIT_NOTNULL, nonNullValueType, this
            ).wrapWithImplicitCast(expectedType)
        }

        if (!KotlinTypeChecker.DEFAULT.isSubtypeOf(valueType.makeNotNullable(), expectedType)) {
            return IrTypeOperatorCallImpl(this.startOffset, this.endOffset, expectedType,
                                          IrTypeOperator.IMPLICIT_CAST, expectedType, this)
        }

        return this
    }
}

