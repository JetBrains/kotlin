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
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.types.impl.originalKotlinType
import org.jetbrains.kotlin.ir.util.TypeTranslator
import org.jetbrains.kotlin.ir.util.coerceToUnitIfNeeded
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext
import org.jetbrains.kotlin.psi2ir.generators.GeneratorExtensions
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.isDynamic
import org.jetbrains.kotlin.types.isNullabilityFlexible
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable

fun insertImplicitCasts(element: IrElement, context: GeneratorContext) {
    element.transformChildren(
        InsertImplicitCasts(context.builtIns, context.irBuiltIns, context.typeTranslator, context.extensions.samConversion),
        null
    )
}

open class InsertImplicitCasts(
    private val builtIns: KotlinBuiltIns,
    private val irBuiltIns: IrBuiltIns,
    private val typeTranslator: TypeTranslator,
    private val samConversion: GeneratorExtensions.SamConversion
) : IrElementTransformerVoid() {

//    override fun visitCallableReference(expression: IrCallableReference): IrExpression =
//        expression.transformPostfix {
//            transformReceiverArguments()
//        }


    private fun getDeclarationSideTypeParameters(declaration: IrFunction): List<IrTypeParameterSymbol> {
        return run {
            declaration.typeParameters + if (declaration is IrConstructor) declaration.parentAsClass.typeParameters else emptyList()
        }.map { it.symbol }
    }

    private fun getTypeSideTypeParameters(declaration: IrFunction): List<IrTypeParameterSymbol> {
        return declaration.dispatchReceiverParameter?.run {
            extractTypeParameters(declaration.parentAsClass).map { it.symbol }
        } ?: emptyList()
    }

    private fun getTypeArguments(expression: IrMemberAccessExpression, declarationTypeParameters: List<IrTypeParameterSymbol>): List<IrTypeArgument> {

        val expressionTypeArguments =
            declarationTypeParameters.map { p -> makeTypeProjection(expression.getTypeArgument(p.owner.index)!!, p.owner.variance) }

        val receiverTypeArguments = expression.dispatchReceiver?.type?.let { (it as? IrSimpleType)?.arguments } ?: emptyList()

        return expressionTypeArguments + receiverTypeArguments
    }

    private fun IrType.substitute(typeParameters: List<IrTypeParameterSymbol>, typeArguments: List<IrTypeArgument>): IrType {
        return IrTypeSubstitutor(typeParameters.distinct(), typeArguments, irBuiltIns).substitute(this)
    }

    override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
        return expression.transformPostfix {
            val declaration = symbol.owner
            val dTypeParameters = getDeclarationSideTypeParameters(declaration)
            val cTypeParameters = getTypeSideTypeParameters(declaration)
            val typeArguments = getTypeArguments(expression, dTypeParameters)
            dispatchReceiver = dispatchReceiver?.cast(declaration.dispatchReceiverParameter?.type?.substitute(dTypeParameters + cTypeParameters, typeArguments))
            extensionReceiver = extensionReceiver?.cast(declaration.extensionReceiverParameter?.type?.substitute(dTypeParameters + cTypeParameters, typeArguments))
        }
    }

    override fun visitPropertyReference(expression: IrPropertyReference): IrExpression {
        return expression.transformPostfix {
            val dispatchReceiver = expression.run { getter?.owner?.dispatchReceiverParameter ?: setter?.owner?.dispatchReceiverParameter }
            val extensionReceiver = expression.run { getter?.owner?.extensionReceiverParameter ?: setter?.owner?.extensionReceiverParameter }
            this.dispatchReceiver = this.dispatchReceiver?.cast(dispatchReceiver?.type)
            this.extensionReceiver = this.extensionReceiver?.cast(extensionReceiver?.type)
        }
    }

    override fun visitLocalDelegatedPropertyReference(expression: IrLocalDelegatedPropertyReference): IrExpression {
        return expression.transformPostfix {
            val declaration = expression.getter.owner
            val dTypeParameters = getDeclarationSideTypeParameters(declaration)
            val cTypeParameters = getTypeSideTypeParameters(declaration)
            val typeArguments = getTypeArguments(expression, dTypeParameters)
            val dispatchReceiver = declaration.dispatchReceiverParameter
            val extensionReceiver = declaration.extensionReceiverParameter
            this.dispatchReceiver = this.dispatchReceiver?.cast(dispatchReceiver?.type?.substitute(dTypeParameters + cTypeParameters, typeArguments))
            this.extensionReceiver = this.extensionReceiver?.cast(extensionReceiver?.type?.substitute(dTypeParameters + cTypeParameters, typeArguments))
        }
    }

    private fun IrMemberAccessExpression.transformReceiverArguments() {
        val declaration = (this as IrFunctionAccessExpression).symbol.owner
        val dTypeParameters = getDeclarationSideTypeParameters(declaration)
        val cTypeParameters = getTypeSideTypeParameters(declaration)
        val typeArguments = getTypeArguments(this, dTypeParameters)
        dispatchReceiver = dispatchReceiver?.cast(declaration.dispatchReceiverParameter?.type?.substitute(dTypeParameters + cTypeParameters, typeArguments))
        extensionReceiver = extensionReceiver?.cast(declaration.extensionReceiverParameter?.type?.substitute(dTypeParameters + cTypeParameters, typeArguments))
    }

    override fun visitMemberAccess(expression: IrMemberAccessExpression): IrExpression =
        with (expression as IrFunctionAccessExpression) {
            val declaration = symbol.owner
            val dTypeParameters = getDeclarationSideTypeParameters(declaration)
            val cTypeParameters = getTypeSideTypeParameters(declaration)
            val typeArguments = getTypeArguments(expression, dTypeParameters)
            transformPostfix {
                transformReceiverArguments()
                for (index in declaration.valueParameters.indices) {
                    val argument = getValueArgument(index) ?: continue
                    val parameterType = declaration.valueParameters[index].type.substitute(dTypeParameters + cTypeParameters, typeArguments)
                    putValueArgument(index, argument.cast(parameterType))
                }
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
            value = if (expression.returnTargetSymbol is IrConstructorSymbol) {
                value.coerceToUnit()
            } else {
                value.cast(with(expression.returnTargetSymbol.owner as IrFunction) { returnType })
            }
        }

    override fun visitSetVariable(expression: IrSetVariable): IrExpression =
        expression.transformPostfix {
            value = value.cast(expression.symbol.owner.type)
        }

    override fun visitSetField(expression: IrSetField): IrExpression =
        expression.transformPostfix {
            value = value.cast(expression.symbol.owner.type)
        }

    override fun visitVariable(declaration: IrVariable): IrVariable =
        declaration.transformPostfix {
            initializer = initializer?.cast(declaration.symbol.owner.type)
        }

    override fun visitField(declaration: IrField): IrStatement =
        declaration.transformPostfix {
            initializer?.coerceInnerExpression(symbol.owner.type)
        }

    override fun visitFunction(declaration: IrFunction): IrStatement =
        typeTranslator.buildWithScope(declaration) {
            declaration.transformPostfix {
                valueParameters.forEach {
                    it.defaultValue?.coerceInnerExpression(it.type)
                }
            }
        }

    override fun visitClass(declaration: IrClass): IrStatement =
        typeTranslator.buildWithScope(declaration) {
            super.visitClass(declaration)
        }

    override fun visitWhen(expression: IrWhen): IrExpression =
        expression.transformPostfix {
            for (irBranch in branches) {
                irBranch.condition = irBranch.condition.cast(irBuiltIns.booleanType)
                irBranch.result = irBranch.result.cast(type)
            }
        }

    override fun visitLoop(loop: IrLoop): IrExpression =
        loop.transformPostfix {
            condition = condition.cast(irBuiltIns.booleanType)
            body = body?.coerceToUnit()
        }

    override fun visitThrow(expression: IrThrow): IrExpression =
        expression.transformPostfix {
            value = value.cast(irBuiltIns.throwableType)
        }

    override fun visitTry(aTry: IrTry): IrExpression =
        aTry.transformPostfix {
            tryResult = tryResult.cast(type)

            for (aCatch in catches) {
                aCatch.result = aCatch.result.cast(type)
            }

            finallyExpression = finallyExpression?.coerceToUnit()
        }

    override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression =
        when (expression.operator) {
            IrTypeOperator.IMPLICIT_CAST ->
                expression.argument.cast(expression.typeOperand)
            else ->
                super.visitTypeOperator(expression)
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

    private fun IrExpressionBody.coerceInnerExpression(expectedType: IrType) {
        expression = expression.cast(expectedType)
    }

    private fun IrExpression.cast(expectedType: IrType?): IrExpression {
        if (expectedType == null) return this
        if (expectedType is IrErrorType) return this

        val notNullableExpectedType = expectedType.makeNotNull()

        val valueType = this.type
        val valueKotlinType = valueType.originalKotlinType!!
        val expectedKotlinType = expectedType.originalKotlinType!!

        return when {
            expectedType.isUnit() -> {
                coerceToUnit()
            }

            valueType is IrDynamicType && expectedType !is IrDynamicType -> {
                require(valueKotlinType.isDynamic() && !expectedKotlinType.isDynamic())
                if (expectedType.isNullableAny()) {
                    this
                } else {
                    implicitCast(expectedType, IrTypeOperator.IMPLICIT_CAST)
                }
            }

            valueKotlinType.isNullabilityFlexible() && valueType.containsNull() && !expectedType.containsNull() -> {
                implicitNonNull(valueType, expectedType)
            }

            valueType.isSubtypeOf(expectedType.makeNullable(), irBuiltIns) -> {
                this
            }

            valueType.isInt() && notNullableExpectedType.isBuiltInIntegerType() -> {
                implicitCast(notNullableExpectedType, IrTypeOperator.IMPLICIT_INTEGER_COERCION)
            }

            valueType.isSubtypeOf(expectedType, irBuiltIns) -> {
                require(valueType.isSubtypeOf(expectedType, irBuiltIns))
                this
            }

            else -> {
                val targetType = if (!valueType.containsNull()) notNullableExpectedType else expectedType
                implicitCast(targetType, IrTypeOperator.IMPLICIT_CAST)
            }
        }
    }

    private fun IrExpression.implicitNonNull(valueType: IrType, expectedType: IrType): IrExpression {
        val notNullValueType = valueType.makeNotNull()
        return implicitCast(notNullValueType, IrTypeOperator.IMPLICIT_NOTNULL).cast(expectedType)
    }

    private fun IrExpression.implicitCast(
        targetType: IrType,
        typeOperator: IrTypeOperator
    ): IrExpression {
        return IrTypeOperatorCallImpl(
            startOffset,
            endOffset,
            targetType,
            typeOperator,
            targetType, targetType.classifierOrFail,
            this
        )
    }

    protected open fun IrExpression.coerceToUnit(): IrExpression {
        val valueType = getKotlinType(this)
        return coerceToUnitIfNeeded(valueType, irBuiltIns)
    }

    protected fun getKotlinType(irExpression: IrExpression) =
        irExpression.type.originalKotlinType!!

    private fun KotlinType.isBuiltInIntegerType(): Boolean =
        KotlinBuiltIns.isByte(this) ||
                KotlinBuiltIns.isShort(this) ||
                KotlinBuiltIns.isInt(this) ||
                KotlinBuiltIns.isLong(this) ||
                KotlinBuiltIns.isUByte(this) ||
                KotlinBuiltIns.isUShort(this) ||
                KotlinBuiltIns.isUInt(this) ||
                KotlinBuiltIns.isULong(this)

    private fun IrType.isBuiltInIntegerType(): Boolean =
        isByte() || isShort() || isInt() || isLong() ||
                isUByte() ||
                isUShort() ||
                isUInt() ||
                isULong()
}

