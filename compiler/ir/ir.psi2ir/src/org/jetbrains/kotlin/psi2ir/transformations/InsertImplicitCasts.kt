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
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.builtins.isSuspendFunctionType
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.impl.originalKotlinType
import org.jetbrains.kotlin.ir.util.TypeTranslator
import org.jetbrains.kotlin.ir.util.coerceToUnitIfNeeded
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.psi2ir.containsNull
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext
import org.jetbrains.kotlin.psi2ir.generators.GeneratorExtensions
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.typeUtil.*

fun insertImplicitCasts(element: IrElement, context: GeneratorContext) {
    InsertImplicitCasts(
        context.builtIns,
        context.irBuiltIns,
        context.typeTranslator,
        context.callToSubstitutedDescriptorMap,
        context.extensions
    ).run(element)
}

internal class InsertImplicitCasts(
    private val builtIns: KotlinBuiltIns,
    private val irBuiltIns: IrBuiltIns,
    private val typeTranslator: TypeTranslator,
    private val callToSubstitutedDescriptorMap: Map<IrMemberAccessExpression, CallableDescriptor>,
    private val generatorExtensions: GeneratorExtensions
) : IrElementTransformerVoid() {

    private val expectedFunctionExpressionReturnType = hashMapOf<FunctionDescriptor, IrType>()
    private val returnExpressionsToBePostprocessed = arrayListOf<IrReturn>()

    fun run(element: IrElement) {
        element.transformChildrenVoid(this)
        for (irReturn in returnExpressionsToBePostprocessed) {
            postprocessReturnExpression(irReturn)
        }
    }

    private fun postprocessReturnExpression(irReturn: IrReturn) {
        val returnTarget = irReturn.returnTarget
        val expectedReturnType = expectedFunctionExpressionReturnType[returnTarget] ?: return
        irReturn.value = irReturn.value.cast(expectedReturnType)
    }

    private fun KotlinType.toIrType() = typeTranslator.translateType(this)

    private val IrMemberAccessExpression.substitutedDescriptor
        get() = callToSubstitutedDescriptorMap[this] ?: symbol.descriptor as CallableDescriptor

    override fun visitCallableReference(expression: IrCallableReference): IrExpression {
        val substitutedDescriptor = expression.substitutedDescriptor
        return expression.transformPostfix {
            transformReceiverArguments(substitutedDescriptor)
        }
    }

    private fun IrMemberAccessExpression.transformReceiverArguments(substitutedDescriptor: CallableDescriptor) {
        dispatchReceiver = dispatchReceiver?.cast(getEffectiveDispatchReceiverType(substitutedDescriptor))
        extensionReceiver = extensionReceiver?.cast(substitutedDescriptor.extensionReceiverParameter?.type)
    }

    private fun getEffectiveDispatchReceiverType(descriptor: CallableDescriptor): KotlinType? =
        when {
            descriptor !is CallableMemberDescriptor ->
                null

            descriptor.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE -> {
                val containingDeclaration = descriptor.containingDeclaration
                if (containingDeclaration !is ClassDescriptor)
                    throw AssertionError("Containing declaration for $descriptor should be a class: $containingDeclaration")
                else
                    containingDeclaration.defaultType.replaceArgumentsWithStarProjections()
            }

            else ->
                descriptor.dispatchReceiverParameter?.type
        }

    override fun visitMemberAccess(expression: IrMemberAccessExpression): IrExpression {
        val substitutedDescriptor = expression.substitutedDescriptor
        return expression.transformPostfix {
            transformReceiverArguments(substitutedDescriptor)
            for (index in substitutedDescriptor.valueParameters.indices) {
                val argument = getValueArgument(index) ?: continue
                val parameterType = substitutedDescriptor.valueParameters[index].type

                // Hack to support SAM conversions on out-projected types.
                // See SamType#createByValueParameter and genericSamProjectedOut.kt for more details.
                val expectedType =
                    if (argument.isSamConversion() && KotlinBuiltIns.isNothing(parameterType))
                        substitutedDescriptor.original.valueParameters[index].type.replaceArgumentsWithNothing()
                    else
                        parameterType

                putValueArgument(index, argument.cast(expectedType))
            }
        }
    }

    private fun IrExpression.isSamConversion(): Boolean =
        this is IrTypeOperatorCall && operator == IrTypeOperator.SAM_CONVERSION

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
                returnExpressionsToBePostprocessed.add(expression)
                value.cast(expression.returnTarget.returnType)
            }
        }

    override fun visitSetVariable(expression: IrSetVariable): IrExpression =
        expression.transformPostfix {
            value = value.cast(expression.symbol.descriptor.type)
        }

    override fun visitGetField(expression: IrGetField): IrExpression =
        expression.transformPostfix {
            receiver = receiver?.cast(getEffectiveDispatchReceiverType(expression.symbol.descriptor))
        }

    override fun visitSetField(expression: IrSetField): IrExpression =
        expression.transformPostfix {
            receiver = receiver?.cast(getEffectiveDispatchReceiverType(expression.symbol.descriptor))
            value = value.cast(expression.symbol.descriptor.type)
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
        typeTranslator.buildWithScope(declaration) {
            declaration.transformPostfix {
                valueParameters.forEach {
                    it.defaultValue?.coerceInnerExpression(it.descriptor.type)
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

    private fun KotlinType.getSubstitutedFunctionTypeForSamType() =
        generatorExtensions.samConversion.getSubstitutedFunctionTypeForSamType(this)

    override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression =
        when (expression.operator) {
            IrTypeOperator.SAM_CONVERSION -> expression.transformPostfix {
                argument = argument.cast(typeOperand.originalKotlinType!!.getSubstitutedFunctionTypeForSamType())
            }

            IrTypeOperator.IMPLICIT_CAST -> {
                // This branch is required for handling specific ambiguous cases in implicit cast insertion,
                // such as SAM conversion VS smart cast.
                // Here IMPLICIT_CAST serves as a type hint.
                // Replace IrTypeOperatorCall(IMPLICIT_CAST, ...) with an argument cast to the required type
                // (possibly generating another IrTypeOperatorCall(IMPLICIT_CAST, ...), if required).

                expression.transformChildrenVoid()
                expression.argument.cast(expression.typeOperand)
            }

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

    private fun IrExpressionBody.coerceInnerExpression(expectedType: KotlinType) {
        expression = expression.cast(expectedType)
    }

    private fun IrExpression.cast(irType: IrType): IrExpression =
        cast(irType.originalKotlinType)

    private fun KotlinType.getFunctionReturnTypeOrNull(): KotlinType? =
        if (isFunctionType || isSuspendFunctionType)
            arguments.last().type
        else
            null

    private fun IrExpression.cast(expectedType: KotlinType?): IrExpression {
        if (expectedType == null) return this
        if (expectedType.isError) return this

        if (this is IrFunctionExpression) {
            val expectedFunctionReturnType = expectedType.getFunctionReturnTypeOrNull()
            if (expectedFunctionReturnType != null) {
                expectedFunctionExpressionReturnType[function.descriptor] = expectedFunctionReturnType.toIrType()
            }
        }

        // TODO here we can have non-denotable KotlinTypes (both in 'this@cast.type' and 'expectedType').

        val notNullableExpectedType = expectedType.makeNotNullable()

        val valueType = this.type.originalKotlinType!!

        return when {
            expectedType.isUnit() ->
                coerceToUnit()

            valueType.isDynamic() && !expectedType.isDynamic() ->
                if (expectedType.isNullableAny())
                    this
                else
                    implicitCast(expectedType, IrTypeOperator.IMPLICIT_DYNAMIC_CAST)

            (valueType.isNullabilityFlexible() && valueType.containsNull() || valueType.hasEnhancedNullability()) &&
                    !expectedType.containsNull() ->
                implicitNonNull(valueType, expectedType)

            KotlinTypeChecker.DEFAULT.isSubtypeOf(valueType, expectedType.makeNullable()) ->
                this

            KotlinBuiltIns.isInt(valueType) && notNullableExpectedType.isBuiltInIntegerType() ->
                implicitCast(notNullableExpectedType, IrTypeOperator.IMPLICIT_INTEGER_COERCION)

            else -> {
                val targetType = if (!valueType.containsNull()) notNullableExpectedType else expectedType
                implicitCast(targetType, IrTypeOperator.IMPLICIT_CAST)
            }
        }
    }

    private fun KotlinType.hasEnhancedNullability() =
        generatorExtensions.enhancedNullability.hasEnhancedNullability(this)

    private fun IrExpression.implicitNonNull(valueType: KotlinType, expectedType: KotlinType): IrExpression {
        val nonNullFlexibleType = valueType.upperIfFlexible().makeNotNullable()
        val nonNullValueType = generatorExtensions.enhancedNullability.stripEnhancedNullability(nonNullFlexibleType)
        return implicitCast(nonNullValueType, IrTypeOperator.IMPLICIT_NOTNULL).cast(expectedType)
    }

    private fun IrExpression.implicitCast(
        targetType: KotlinType,
        typeOperator: IrTypeOperator
    ): IrExpression {
        val irType = targetType.toIrType()
        return IrTypeOperatorCallImpl(
            startOffset,
            endOffset,
            irType,
            typeOperator,
            irType,
            this
        )
    }

    private fun IrExpression.coerceToUnit(): IrExpression {
        val valueType = getKotlinType(this)
        return coerceToUnitIfNeeded(valueType, irBuiltIns)
    }

    private fun getKotlinType(irExpression: IrExpression) =
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
}
