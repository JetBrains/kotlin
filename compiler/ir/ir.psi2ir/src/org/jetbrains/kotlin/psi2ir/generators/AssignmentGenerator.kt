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

package org.jetbrains.kotlin.psi2ir.generators

import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.descriptors.impl.SyntheticFieldDescriptor
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrDynamicOperatorExpressionImpl
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffsetSkippingComments
import org.jetbrains.kotlin.psi2ir.intermediate.*
import org.jetbrains.kotlin.psi2ir.resolveFakeOverride
import org.jetbrains.kotlin.psi2ir.unwrappedGetMethod
import org.jetbrains.kotlin.psi2ir.unwrappedSetMethod
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.PropertyImportedFromObject
import org.jetbrains.kotlin.resolve.calls.util.isSafeCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.tasks.isDynamic
import org.jetbrains.kotlin.resolve.calls.util.FakeCallableDescriptorForObject
import org.jetbrains.kotlin.types.KotlinType

class AssignmentGenerator(statementGenerator: StatementGenerator) : StatementGeneratorExtension(statementGenerator) {

    fun generateAssignment(ktExpression: KtBinaryExpression): IrExpression {
        val ktLeft = ktExpression.left!!
        val irRhs = ktExpression.right!!.genExpr()
        val irAssignmentReceiver = generateAssignmentReceiver(ktLeft, IrStatementOrigin.EQ)
        return irAssignmentReceiver.assign(irRhs)
    }

    fun generateAugmentedAssignment(ktExpression: KtBinaryExpression, origin: IrStatementOrigin): IrExpression {
        val opResolvedCall = getResolvedCall(ktExpression)!!
        val isSimpleAssignment = get(BindingContext.VARIABLE_REASSIGNMENT, ktExpression) ?: false
        val ktLeft = ktExpression.left!!
        val ktRight = ktExpression.right!!
        val irAssignmentReceiver = generateAssignmentReceiver(ktLeft, origin)
        val isDynamicCall = opResolvedCall.resultingDescriptor.isDynamic()

        return irAssignmentReceiver.assign { irLValue ->
            if (isDynamicCall) {
                IrDynamicOperatorExpressionImpl(
                    ktExpression.startOffsetSkippingComments, ktExpression.endOffset,
                    context.irBuiltIns.unitType,
                    getDynamicAugmentedAssignmentOperator(ktExpression.operationToken)
                ).apply {
                    left = irLValue.load()
                    right = ktRight.genExpr()
                }
            } else {
                val opCall = statementGenerator.pregenerateCallReceivers(opResolvedCall)
                opCall.setExplicitReceiverValue(irLValue)
                opCall.irValueArgumentsByIndex[0] = ktRight.genExpr()
                statementGenerator.generateSamConversionForValueArgumentsIfRequired(opCall, opResolvedCall)
                val irOpCall = CallGenerator(statementGenerator).generateCall(ktExpression, opCall, origin)

                if (isSimpleAssignment) {
                    // Set( Op( Get(), RHS ) )
                    irLValue.store(irOpCall)
                } else {
                    // Op( Get(), RHS )
                    irOpCall
                }
            }
        }
    }

    private fun getDynamicAugmentedAssignmentOperator(operatorToken: IElementType): IrDynamicOperator =
        when (operatorToken) {
            KtTokens.PLUSEQ -> IrDynamicOperator.PLUSEQ
            KtTokens.MINUSEQ -> IrDynamicOperator.MINUSEQ
            KtTokens.MULTEQ -> IrDynamicOperator.MULEQ
            KtTokens.DIVEQ -> IrDynamicOperator.DIVEQ
            KtTokens.PERCEQ -> IrDynamicOperator.MODEQ
            else -> throw AssertionError("Unexpected operator token: $operatorToken")
        }

    fun generatePrefixIncrementDecrement(ktExpression: KtPrefixExpression, origin: IrStatementOrigin): IrExpression {
        val opResolvedCall = getResolvedCall(ktExpression)!!
        val ktBaseExpression = ktExpression.baseExpression!!
        val irAssignmentReceiver = generateAssignmentReceiver(ktBaseExpression, origin, isAssignmentStatement = false)
        val isDynamicCall = opResolvedCall.resultingDescriptor.isDynamic()

        return irAssignmentReceiver.assign { irLValue ->
            val startOffset = ktExpression.startOffsetSkippingComments
            val endOffset = ktExpression.endOffset

            if (isDynamicCall) {
                IrDynamicOperatorExpressionImpl(
                    startOffset, endOffset,
                    irLValue.type,
                    if (ktExpression.operationToken == KtTokens.PLUSPLUS)
                        IrDynamicOperator.PREFIX_INCREMENT
                    else
                        IrDynamicOperator.PREFIX_DECREMENT
                ).apply {
                    receiver = irLValue.load()
                }
            } else {
                irBlock(startOffset, endOffset, origin, irLValue.type) {
                    val opCall = statementGenerator.pregenerateCall(opResolvedCall)
                    opCall.setExplicitReceiverValue(irLValue)
                    val irOpCall = CallGenerator(statementGenerator).generateCall(ktExpression, opCall, origin)
                    +irLValue.store(irOpCall)
                    +irLValue.load()
                }
            }
        }
    }

    fun generatePostfixIncrementDecrement(ktExpression: KtPostfixExpression, origin: IrStatementOrigin): IrExpression {
        val opResolvedCall = getResolvedCall(ktExpression)!!
        val ktBaseExpression = ktExpression.baseExpression!!
        val irAssignmentReceiver = generateAssignmentReceiver(ktBaseExpression, origin, isAssignmentStatement = false)
        val isDynamicCall = opResolvedCall.resultingDescriptor.isDynamic()
        val startOffset = ktExpression.startOffsetSkippingComments
        val endOffset = ktExpression.endOffset

        return irAssignmentReceiver.assign { irLValue ->
            if (isDynamicCall) {
                IrDynamicOperatorExpressionImpl(
                    startOffset, endOffset,
                    irLValue.type,
                    if (ktExpression.operationToken == KtTokens.PLUSPLUS)
                        IrDynamicOperator.POSTFIX_INCREMENT
                    else
                        IrDynamicOperator.POSTFIX_DECREMENT
                ).apply {
                    receiver = irLValue.load()
                }
            } else {
                irBlock(startOffset, endOffset, origin, irLValue.type) {
                    val temporary = irTemporary(irLValue.load())
                    val opCall = statementGenerator.pregenerateCall(opResolvedCall)
                    opCall.setExplicitReceiverValue(
                        VariableLValue(context, startOffset, endOffset, temporary.symbol, temporary.type)
                    )
                    val irOpCall = CallGenerator(statementGenerator).generateCall(ktExpression, opCall, origin)
                    +irLValue.store(irOpCall)
                    +irGet(temporary.type, temporary.symbol)
                }
            }
        }
    }

    private fun generateAssignmentReceiver(
        ktLeft: KtExpression,
        origin: IrStatementOrigin,
        isAssignmentStatement: Boolean = true
    ): AssignmentReceiver {
        val ktExpr = KtPsiUtil.safeDeparenthesize(ktLeft)
        if (ktExpr is KtArrayAccessExpression) {
            return generateArrayAccessAssignmentReceiver(ktExpr, origin)
        }

        val resolvedCall = getResolvedCall(ktExpr)
            ?: return generateExpressionAssignmentReceiver(ktExpr, origin, isAssignmentStatement)
        val descriptor = resolvedCall.resultingDescriptor

        val startOffset = ktExpr.startOffsetSkippingComments
        val endOffset = ktExpr.endOffset
        return when (descriptor) {
            is SyntheticFieldDescriptor -> {
                val receiverValue =
                    statementGenerator.generateBackingFieldReceiver(
                        startOffset, endOffset,
                        resolvedCall,
                        descriptor
                    )
                createBackingFieldLValue(ktExpr, descriptor.propertyDescriptor, receiverValue, origin)
            }
            is LocalVariableDescriptor ->
                if (descriptor.isDelegated)
                    DelegatedLocalPropertyLValue(
                        context,
                        startOffset, endOffset,
                        descriptor.type.toIrType(),
                        descriptor.getter?.let { context.symbolTable.referenceDeclaredFunction(it) },
                        descriptor.setter?.let { context.symbolTable.referenceDeclaredFunction(it) },
                        origin
                    )
                else
                    createVariableValue(ktExpr, descriptor, origin)
            is PropertyDescriptor ->
                generateAssignmentReceiverForProperty(descriptor, origin, ktExpr, resolvedCall, isAssignmentStatement)
            is FakeCallableDescriptorForObject ->
                OnceExpressionValue(ktExpr.genExpr())
            is ValueDescriptor ->
                createVariableValue(ktExpr, descriptor, origin)
            else ->
                OnceExpressionValue(ktExpr.genExpr())
        }
    }

    private fun generateExpressionAssignmentReceiver(
        ktLeft: KtExpression,
        origin: IrStatementOrigin,
        isAssignmentStatement: Boolean
    ): AssignmentReceiver {
        // This is a somewhat special case when LHS of the augmented assignment operator is an arbitrary expression without resolved call.
        // This can happen only in case of compound assignment resolved to '<op>Assign' operator, e.g.,
        //      (a as MutableList<Any>) += 42
        if (!isAssignmentStatement) {
            throw AssertionError("Arbitrary assignment receiver found in assignment-like expression: ${ktLeft.parent.text}")
        }

        return SpecialExpressionAssignmentReceiver(
            statementGenerator, ktLeft, origin,
            context.bindingContext.getType(ktLeft)?.toIrType() ?: throw AssertionError("No type for expression ${ktLeft.text}")
        )
    }

    private fun createVariableValue(
        ktExpression: KtExpression,
        descriptor: ValueDescriptor,
        origin: IrStatementOrigin
    ) =
        VariableLValue(
            context,
            ktExpression.startOffsetSkippingComments, ktExpression.endOffset,
            context.symbolTable.referenceValue(descriptor),
            descriptor.type.toIrType(),
            origin
        )

    private fun createBackingFieldLValue(
        ktExpression: KtExpression,
        descriptor: PropertyDescriptor,
        receiverValue: IntermediateValue?,
        origin: IrStatementOrigin?
    ): BackingFieldLValue =
        BackingFieldLValue(
            context,
            ktExpression.startOffsetSkippingComments, ktExpression.endOffset,
            descriptor.type.toIrType(),
            context.symbolTable.referenceField(descriptor),
            receiverValue, origin
        )

    private fun generateAssignmentReceiverForProperty(
        descriptor: PropertyDescriptor,
        origin: IrStatementOrigin,
        ktLeft: KtExpression,
        resolvedCall: ResolvedCall<*>,
        isAssignmentStatement: Boolean
    ): AssignmentReceiver =
        when {
            descriptor.isDynamic() ->
                DynamicMemberLValue(
                    context,
                    ktLeft.startOffsetSkippingComments, ktLeft.endOffset,
                    descriptor.type.toIrType(),
                    descriptor.name.asString(),
                    statementGenerator.generateCallReceiver(
                        ktLeft, descriptor, resolvedCall.dispatchReceiver,
                        resolvedCall.extensionReceiver,
                        resolvedCall.contextReceivers,
                        isSafe = resolvedCall.call.isSafeCall(),
                        isAssignmentReceiver = isAssignmentStatement
                    )
                )
            origin == IrStatementOrigin.EQ && !descriptor.isVar -> {
                // An assignment to a val property can only be its initialization in the constructor.
                val receiver = resolvedCall.dispatchReceiver ?: descriptor.dispatchReceiverParameter?.value
                createBackingFieldLValue(ktLeft, descriptor, statementGenerator.generateReceiverOrNull(ktLeft, receiver), null)
            }
            else -> {
                val propertyReceiver = statementGenerator.generateCallReceiver(
                    ktLeft, descriptor, resolvedCall.dispatchReceiver,
                    resolvedCall.extensionReceiver,
                    resolvedCall.contextReceivers,
                    isSafe = resolvedCall.call.isSafeCall(),
                    isAssignmentReceiver = isAssignmentStatement
                )

                val superQualifier = getSuperQualifier(resolvedCall)
                val candidateDescriptor = resolvedCall.candidateDescriptor as PropertyDescriptor

                // TODO property imported from an object
                createPropertyLValue(
                    ktLeft, descriptor, candidateDescriptor, propertyReceiver, getTypeArguments(resolvedCall), origin, superQualifier
                )
            }
        }

    private fun PropertyDescriptor.unwrapPropertyDescriptor() =
        when (this) {
            is PropertyImportedFromObject -> callableFromObject
            else -> this
        }

    private fun createPropertyLValue(
        ktExpression: KtExpression,
        resultingDescriptor: PropertyDescriptor,
        candidateDescriptor: PropertyDescriptor,
        propertyReceiver: CallReceiver,
        typeArgumentsMap: Map<TypeParameterDescriptor, KotlinType>?,
        origin: IrStatementOrigin?,
        superQualifier: ClassDescriptor?
    ): PropertyLValueBase {

        val unwrappedPropertyDescriptor = resultingDescriptor.unwrapPropertyDescriptor()
        val getterDescriptor = unwrappedPropertyDescriptor.unwrappedGetMethod
        val setterDescriptor = unwrappedPropertyDescriptor.unwrappedSetMethod
            ?.takeUnless { it.visibility == DescriptorVisibilities.INVISIBLE_FAKE }

        val getterSymbol = getterDescriptor?.let { context.symbolTable.referenceSimpleFunction(it.original) }
        val setterSymbol = setterDescriptor?.let { context.symbolTable.referenceSimpleFunction(it.original) }

        val propertyIrType = resultingDescriptor.type.toIrType()
        return if (getterSymbol != null || setterSymbol != null) {
            val superQualifierSymbol = superQualifier?.let { context.symbolTable.referenceClass(it) }
            val typeArgumentsList =
                typeArgumentsMap?.let { typeArguments ->
                    candidateDescriptor.typeParameters.map {
                        typeArguments[it]!!.toIrType()
                    }
                }
            AccessorPropertyLValue(
                context,
                scope,
                ktExpression.startOffsetSkippingComments, ktExpression.endOffset, origin,
                propertyIrType,
                getterSymbol,
                getterDescriptor?.let {
                    computeSubstitutedSyntheticAccessor(unwrappedPropertyDescriptor, it, unwrappedPropertyDescriptor.getter!!)
                },
                setterSymbol,
                setterDescriptor?.let {
                    computeSubstitutedSyntheticAccessor(unwrappedPropertyDescriptor, it, unwrappedPropertyDescriptor.setter!!)
                },
                typeArgumentsList,
                propertyReceiver,
                superQualifierSymbol
            )
        } else {
            val superQualifierSymbol = (superQualifier
                ?: unwrappedPropertyDescriptor.containingDeclaration as? ClassDescriptor)?.let { context.symbolTable.referenceClass(it) }
            FieldPropertyLValue(
                context,
                scope,
                ktExpression.startOffsetSkippingComments, ktExpression.endOffset, origin,
                context.symbolTable.referenceField(unwrappedPropertyDescriptor.resolveFakeOverride().original),
                unwrappedPropertyDescriptor,
                propertyIrType,
                propertyReceiver,
                superQualifierSymbol
            )
        }
    }

    private fun generateArrayAccessAssignmentReceiver(
        ktArrayExpr: KtArrayAccessExpression,
        origin: IrStatementOrigin
    ): ArrayAccessAssignmentReceiver {
        val indexedGetResolvedCall = get(BindingContext.INDEXED_LVALUE_GET, ktArrayExpr)
        val indexedSetResolvedCall = get(BindingContext.INDEXED_LVALUE_SET, ktArrayExpr)

        return ArrayAccessAssignmentReceiver(
            ktArrayExpr.arrayExpression!!.genExpr(),
            ktArrayExpr.indexExpressions,
            ktArrayExpr.indexExpressions.map { it.genExpr() },
            indexedGetResolvedCall,
            indexedSetResolvedCall,
            { indexedGetResolvedCall?.let { statementGenerator.pregenerateCallReceivers(it) } },
            { indexedSetResolvedCall?.let { statementGenerator.pregenerateCallReceivers(it) } },
            CallGenerator(statementGenerator),
            ktArrayExpr.startOffsetSkippingComments,
            ktArrayExpr.endOffset,
            origin
        )
    }

}
