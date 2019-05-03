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
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.util.referenceFunction
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffsetSkippingComments
import org.jetbrains.kotlin.psi2ir.intermediate.*
import org.jetbrains.kotlin.psi2ir.unwrappedGetMethod
import org.jetbrains.kotlin.psi2ir.unwrappedSetMethod
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.isSafeCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.tasks.isDynamic
import org.jetbrains.kotlin.resolve.scopes.receivers.ThisClassReceiver
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
                statementGenerator.generateSamConversionForValueArgumentsIfRequired(opCall, opResolvedCall.resultingDescriptor)
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
        if (ktLeft is KtArrayAccessExpression) {
            return generateArrayAccessAssignmentReceiver(ktLeft, origin)
        }

        val resolvedCall = getResolvedCall(ktLeft)
            ?: return generateExpressionAssignmentReceiver(ktLeft, origin, isAssignmentStatement)
        val descriptor = resolvedCall.resultingDescriptor

        val startOffset = ktLeft.startOffsetSkippingComments
        val endOffset = ktLeft.endOffset
        return when (descriptor) {
            is SyntheticFieldDescriptor -> {
                val receiverValue =
                    statementGenerator.generateBackingFieldReceiver(
                        startOffset, endOffset,
                        resolvedCall,
                        descriptor
                    )
                createBackingFieldLValue(ktLeft, descriptor.propertyDescriptor, receiverValue, origin)
            }
            is LocalVariableDescriptor ->
                @Suppress("DEPRECATION")
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
                    createVariableValue(ktLeft, descriptor, origin)
            is PropertyDescriptor ->
                generateAssignmentReceiverForProperty(descriptor, origin, ktLeft, resolvedCall, isAssignmentStatement)
            is ValueDescriptor ->
                createVariableValue(ktLeft, descriptor, origin)
            else ->
                OnceExpressionValue(ktLeft.genExpr())
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
                        ktLeft, descriptor, resolvedCall.dispatchReceiver, resolvedCall.extensionReceiver,
                        isSafe = resolvedCall.call.isSafeCall(),
                        isAssignmentReceiver = isAssignmentStatement
                    )
                )
            isValInitializationInConstructor(descriptor, resolvedCall) -> {
                val thisClass = getThisClass()
                val thisAsReceiverParameter = thisClass.thisAsReceiverParameter
                val thisType = thisAsReceiverParameter.type.toIrType()
                val irThis = IrGetValueImpl(
                    ktLeft.startOffsetSkippingComments, ktLeft.endOffset,
                    thisType,
                    context.symbolTable.referenceValueParameter(thisAsReceiverParameter)
                )
                createBackingFieldLValue(ktLeft, descriptor, RematerializableValue(irThis), null)
            }
            else -> {
                val propertyReceiver = statementGenerator.generateCallReceiver(
                    ktLeft, descriptor, resolvedCall.dispatchReceiver, resolvedCall.extensionReceiver,
                    isSafe = resolvedCall.call.isSafeCall(),
                    isAssignmentReceiver = isAssignmentStatement
                )

                val superQualifier = getSuperQualifier(resolvedCall)

                // TODO property imported from an object
                createPropertyLValue(ktLeft, descriptor, propertyReceiver, getTypeArguments(resolvedCall), origin, superQualifier)
            }
        }

    private fun createPropertyLValue(
        ktExpression: KtExpression,
        descriptor: PropertyDescriptor,
        propertyReceiver: CallReceiver,
        typeArgumentsMap: Map<TypeParameterDescriptor, KotlinType>?,
        origin: IrStatementOrigin?,
        superQualifier: ClassDescriptor?
    ): PropertyLValueBase {
        val superQualifierSymbol = superQualifier?.let { context.symbolTable.referenceClass(it) }

        val getterDescriptor = descriptor.unwrappedGetMethod
        val setterDescriptor = descriptor.unwrappedSetMethod

        val getterSymbol = getterDescriptor?.let { context.symbolTable.referenceFunction(it.original) }
        val setterSymbol = setterDescriptor?.let { context.symbolTable.referenceFunction(it.original) }

        val propertyIrType = descriptor.type.toIrType()
        return if (getterSymbol != null || setterSymbol != null) {
            val typeArgumentsList =
                typeArgumentsMap?.let { typeArguments ->
                    descriptor.original.typeParameters.map { typeArguments[it]!!.toIrType() }
                }
            AccessorPropertyLValue(
                context,
                scope,
                ktExpression.startOffsetSkippingComments, ktExpression.endOffset, origin,
                propertyIrType,
                getterSymbol,
                getterDescriptor,
                setterSymbol,
                setterDescriptor,
                typeArgumentsList,
                propertyReceiver,
                superQualifierSymbol
            )
        } else
            FieldPropertyLValue(
                context,
                scope,
                ktExpression.startOffsetSkippingComments, ktExpression.endOffset, origin,
                context.symbolTable.referenceField(descriptor),
                propertyIrType,
                propertyReceiver,
                superQualifierSymbol
            )
    }

    private fun isValInitializationInConstructor(descriptor: PropertyDescriptor, resolvedCall: ResolvedCall<*>): Boolean =
        !descriptor.isVar &&
                descriptor.kind != CallableMemberDescriptor.Kind.FAKE_OVERRIDE &&
                statementGenerator.scopeOwner.let { it is ConstructorDescriptor || it is ClassDescriptor } &&
                resolvedCall.dispatchReceiver is ThisClassReceiver

    private fun getThisClass(): ClassDescriptor {
        val scopeOwner = statementGenerator.scopeOwner
        return when (scopeOwner) {
            is ClassConstructorDescriptor -> scopeOwner.containingDeclaration
            is ClassDescriptor -> scopeOwner
            else -> scopeOwner.containingDeclaration as ClassDescriptor
        }
    }

    private fun generateArrayAccessAssignmentReceiver(
        ktLeft: KtArrayAccessExpression,
        origin: IrStatementOrigin
    ): ArrayAccessAssignmentReceiver {
        val indexedGetResolvedCall = get(BindingContext.INDEXED_LVALUE_GET, ktLeft)
        val indexedSetResolvedCall = get(BindingContext.INDEXED_LVALUE_SET, ktLeft)

        return ArrayAccessAssignmentReceiver(
            ktLeft.arrayExpression!!.genExpr(),
            ktLeft.indexExpressions,
            ktLeft.indexExpressions.map { it.genExpr() },
            indexedGetResolvedCall,
            indexedSetResolvedCall,
            { indexedGetResolvedCall?.let { statementGenerator.pregenerateCallReceivers(it) } },
            { indexedSetResolvedCall?.let { statementGenerator.pregenerateCallReceivers(it) } },
            CallGenerator(statementGenerator),
            ktLeft.startOffsetSkippingComments,
            ktLeft.endOffset,
            origin
        )
    }

}
