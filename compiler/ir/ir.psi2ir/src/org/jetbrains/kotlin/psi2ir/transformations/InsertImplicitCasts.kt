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
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.psi2ir.containsNull
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.isError
import org.jetbrains.kotlin.types.isNullabilityFlexible
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import org.jetbrains.kotlin.types.upperIfFlexible

fun insertImplicitCasts(builtIns: KotlinBuiltIns, element: IrElement) {
    element.transformChildren(InsertImplicitCasts(builtIns), null)
}

class InsertImplicitCasts(val builtIns: KotlinBuiltIns): IrElementTransformerVoid() {
    private inline fun <T : IrElement> T.transformPostfix(body: T.() -> Unit): T {
        transformChildrenVoid(this@InsertImplicitCasts)
        this.body()
        return this
    }

    override fun visitElement(element: IrElement): IrElement {
        element.transformChildrenVoid(this)
        return element
    }

    override fun visitMemberAccess(expression: IrMemberAccessExpression): IrExpression =
            expression.transformPostfix {
                dispatchReceiver = dispatchReceiver?.cast(descriptor.dispatchReceiverParameter?.type)
                extensionReceiver = extensionReceiver?.cast(descriptor.extensionReceiverParameter?.type)
                for (index in descriptor.valueParameters.indices) {
                    val argument = getValueArgument(index) ?: continue
                    val parameterType = descriptor.valueParameters[index].type
                    putValueArgument(index, argument.cast(parameterType))
                }
            }

    override fun visitBlockBody(body: IrBlockBody): IrBody =
            body.transformPostfix {
                statements.forEachIndexed { i, irStatement ->
                    if (irStatement is IrExpression) {
                        body.statements[i] = irStatement.coerceToUnit()
                    }
                }
            }

    override fun visitContainerExpression(expression: IrContainerExpression): IrExpression =
            expression.transformPostfix {
                if (statements.isEmpty()) return this

                val lastIndex = statements.lastIndex
                statements.forEachIndexed { i, irStatement ->
                    if (irStatement is IrExpression) {
                        statements[i] =
                                if (i == lastIndex)
                                    irStatement.cast(type)
                                else
                                    irStatement.coerceToUnit()
                    }
                }
            }

    override fun visitReturn(expression: IrReturn): IrExpression =
            expression.transformPostfix {
                value = value.cast(expression.returnTarget.returnType)
            }

    override fun visitSetVariable(expression: IrSetVariable): IrExpression =
            expression.transformPostfix {
                value = value.cast(expression.descriptor.type)
            }

    override fun visitSetField(expression: IrSetField): IrExpression =
            expression.transformPostfix {
                value = value.cast(expression.descriptor.type)
            }

    override fun visitVariable(declaration: IrVariable): IrVariable =
            declaration.transformPostfix {
                initializer = initializer?.cast(declaration.descriptor.type)
            }

    override fun visitField(declaration: IrField): IrStatement =
            declaration.transformPostfix {
                initializer?.coerceInnerExpression(descriptor.type)
            }

    override fun visitFunction(declaration: IrFunction): IrStatement =
            declaration.transformPostfix {
                descriptor.valueParameters.forEach {
                    getDefault(it)?.coerceInnerExpression(it.type)
                }
            }

    override fun visitWhen(expression: IrWhen): IrExpression =
            expression.transformPostfix {
                for (irBranch in branches) {
                    irBranch.condition = irBranch.condition.cast(builtIns.booleanType)
                    irBranch.result = irBranch.result.cast(type)
                }
            }

    override fun visitLoop(loop: IrLoop): IrExpression =
            loop.transformPostfix {
                condition = condition.cast(builtIns.booleanType)
                body = body?.coerceToUnit()
            }

    override fun visitThrow(expression: IrThrow): IrExpression =
            expression.transformPostfix {
                value = value.cast(builtIns.throwable.defaultType)
            }

    override fun visitTry(aTry: IrTry): IrExpression =
            aTry.transformPostfix {
                tryResult = tryResult.cast(type)

                for (aCatch in catches) {
                    aCatch.result = aCatch.result.cast(type)
                }

                finallyExpression = finallyExpression?.coerceToUnit()
            }

    override fun visitVararg(expression: IrVararg): IrExpression =
            expression.transformPostfix {
                elements.forEachIndexed { i, element ->
                    when (element) {
                        is IrSpreadElement ->
                            element.expression = element.expression.cast(expression.type)
                        is IrExpression ->
                            putElement(i, element.cast(varargElementType))
                    }
                }
            }

    private fun IrExpressionBody.coerceInnerExpression(expectedType: KotlinType) {
        expression = expression.cast(expectedType)
    }

    private fun IrExpression.cast(expectedType: KotlinType?): IrExpression {
        if (expectedType == null) return this
        if (expectedType.isError) return this

        val notNullableExpectedType = expectedType.makeNotNullable()

        val valueType = this.type

        return when {
            KotlinBuiltIns.isUnit(expectedType) ->
                coerceToUnit()
            valueType.isNullabilityFlexible() && valueType.containsNull() && !expectedType.containsNull() -> {
                val nonNullValueType = valueType.upperIfFlexible().makeNotNullable()
                IrTypeOperatorCallImpl(
                        startOffset, endOffset, nonNullValueType,
                        IrTypeOperator.IMPLICIT_NOTNULL, nonNullValueType, this
                ).cast(expectedType)
            }
            KotlinTypeChecker.DEFAULT.isSubtypeOf(valueType.makeNotNullable(), expectedType) ->
                this
            KotlinBuiltIns.isInt(valueType) && notNullableExpectedType.isBuiltInIntegerType() ->
                IrTypeOperatorCallImpl(startOffset, endOffset, notNullableExpectedType,
                                       IrTypeOperator.IMPLICIT_INTEGER_COERCION, notNullableExpectedType, this)
            else ->
                IrTypeOperatorCallImpl(startOffset, endOffset, expectedType,
                                       IrTypeOperator.IMPLICIT_CAST, expectedType, this)
        }
    }

    private fun IrExpression.coerceToUnit(): IrExpression {
        val valueType = this.type

        return if (KotlinTypeChecker.DEFAULT.isSubtypeOf(valueType, builtIns.unitType))
            this
        else
            IrTypeOperatorCallImpl(startOffset, endOffset, builtIns.unitType,
                                   IrTypeOperator.IMPLICIT_COERCION_TO_UNIT, builtIns.unitType, this)
    }

    private fun KotlinType.isBuiltInIntegerType(): Boolean =
            KotlinBuiltIns.isByte(this) ||
            KotlinBuiltIns.isShort(this) ||
            KotlinBuiltIns.isInt(this) ||
            KotlinBuiltIns.isLong(this)
}

