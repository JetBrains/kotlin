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

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.descriptors.impl.SyntheticFieldDescriptor
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi2ir.intermediate.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.isSafeCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.scopes.receivers.ThisClassReceiver
import org.jetbrains.kotlin.types.KotlinType

class AssignmentGenerator(statementGenerator: StatementGenerator) : StatementGeneratorExtension(statementGenerator) {
    fun generateAssignment(expression: KtBinaryExpression): IrExpression {
        val ktLeft = expression.left!!
        val irRhs = statementGenerator.generateExpression(expression.right!!)
        val irAssignmentReceiver = generateAssignmentReceiver(ktLeft, IrStatementOrigin.EQ)
        return irAssignmentReceiver.assign(irRhs)
    }

    fun generateAugmentedAssignment(expression: KtBinaryExpression, origin: IrStatementOrigin): IrExpression {
        val opResolvedCall = getResolvedCall(expression)!!
        val isSimpleAssignment = get(BindingContext.VARIABLE_REASSIGNMENT, expression) ?: false
        val ktLeft = expression.left!!
        val ktRight = expression.right!!
        val irAssignmentReceiver = generateAssignmentReceiver(ktLeft, origin)

        return irAssignmentReceiver.assign { irLValue ->
            val opCall = statementGenerator.pregenerateCallReceivers(opResolvedCall)
            opCall.setExplicitReceiverValue(irLValue)
            opCall.irValueArgumentsByIndex[0] = statementGenerator.generateExpression(ktRight)
            val irOpCall = CallGenerator(statementGenerator).generateCall(expression, opCall, origin)

            if (isSimpleAssignment) {
                // Set( Op( Get(), RHS ) )
                irLValue.store(irOpCall)
            } else {
                // Op( Get(), RHS )
                irOpCall
            }
        }
    }

    fun generatePrefixIncrementDecrement(expression: KtPrefixExpression, origin: IrStatementOrigin): IrExpression {
        val opResolvedCall = getResolvedCall(expression)!!
        val ktBaseExpression = expression.baseExpression!!
        val irAssignmentReceiver = generateAssignmentReceiver(ktBaseExpression, origin)

        return irAssignmentReceiver.assign { irLValue ->
            irBlock(expression, origin, irLValue.type) {
                val opCall = statementGenerator.pregenerateCall(opResolvedCall)
                opCall.setExplicitReceiverValue(irLValue)
                val irOpCall = CallGenerator(statementGenerator).generateCall(expression, opCall, origin)
                +irLValue.store(irOpCall)
                +irLValue.load()
            }
        }
    }

    fun generatePostfixIncrementDecrement(expression: KtPostfixExpression, origin: IrStatementOrigin): IrExpression {
        val opResolvedCall = getResolvedCall(expression)!!
        val ktBaseExpression = expression.baseExpression!!
        val irAssignmentReceiver = generateAssignmentReceiver(ktBaseExpression, origin)

        return irAssignmentReceiver.assign { irLValue ->
            irBlock(expression, origin, irLValue.type) {
                val temporary = irTemporary(irLValue.load())
                val opCall = statementGenerator.pregenerateCall(opResolvedCall)
                opCall.setExplicitReceiverValue(VariableLValue(startOffset, endOffset, temporary.symbol))
                val irOpCall = CallGenerator(statementGenerator).generateCall(expression, opCall, origin)
                +irLValue.store(irOpCall)
                +irGet(temporary.symbol)
            }
        }
    }

    fun generateAssignmentReceiver(ktLeft: KtExpression, origin: IrStatementOrigin): AssignmentReceiver {
        if (ktLeft is KtArrayAccessExpression) {
            return generateArrayAccessAssignmentReceiver(ktLeft, origin)
        }

        val resolvedCall = getResolvedCall(ktLeft) ?: TODO("no resolved call for LHS")
        val descriptor = resolvedCall.resultingDescriptor

        return when (descriptor) {
            is SyntheticFieldDescriptor -> {
                val receiverValue =
                    statementGenerator.generateBackingFieldReceiver(ktLeft.startOffset, ktLeft.endOffset, resolvedCall, descriptor)
                createBackingFieldLValue(ktLeft, descriptor.propertyDescriptor, receiverValue, origin)
            }
            is LocalVariableDescriptor ->
                @Suppress("DEPRECATION")
                if (descriptor.isDelegated)
                    DelegatedLocalPropertyLValue(
                        ktLeft.startOffset, ktLeft.endOffset,
                        descriptor.type,
                        descriptor.getter?.let { context.symbolTable.referenceDeclaredFunction(it) },
                        descriptor.setter?.let { context.symbolTable.referenceDeclaredFunction(it) },
                        origin
                    )
                else
                    VariableLValue(
                        ktLeft.startOffset, ktLeft.endOffset,
                        context.symbolTable.referenceVariable(descriptor),
                        origin
                    )
            is PropertyDescriptor ->
                generateAssignmentReceiverForProperty(descriptor, origin, ktLeft, resolvedCall)
            is ValueDescriptor ->
                VariableLValue(
                    ktLeft.startOffset, ktLeft.endOffset,
                    context.symbolTable.referenceValue(descriptor),
                    origin
                )
            else ->
                OnceExpressionValue(statementGenerator.generateExpression(ktLeft))
        }
    }

    private fun createBackingFieldLValue(
        ktExpression: KtExpression,
        descriptor: PropertyDescriptor,
        receiverValue: IntermediateValue?,
        origin: IrStatementOrigin?
    ): BackingFieldLValue =
        BackingFieldLValue(
            ktExpression.startOffset, ktExpression.endOffset,
            descriptor.type,
            context.symbolTable.referenceField(descriptor),
            receiverValue, origin
        )

    private fun generateAssignmentReceiverForProperty(
        descriptor: PropertyDescriptor,
        origin: IrStatementOrigin,
        ktLeft: KtExpression,
        resolvedCall: ResolvedCall<*>
    ): AssignmentReceiver =
        if (isValInitializationInConstructor(descriptor, resolvedCall)) {
            val thisClass = getThisClass()
            val irThis = IrGetValueImpl(
                ktLeft.startOffset, ktLeft.endOffset,
                context.symbolTable.referenceValueParameter(thisClass.thisAsReceiverParameter)
            )
            createBackingFieldLValue(ktLeft, descriptor, RematerializableValue(irThis), null)
        } else {
            val propertyReceiver = statementGenerator.generateCallReceiver(
                ktLeft, descriptor, resolvedCall.dispatchReceiver, resolvedCall.extensionReceiver,
                isSafe = resolvedCall.call.isSafeCall(),
                isAssignmentReceiver = true
            )

            val superQualifier = getSuperQualifier(resolvedCall)

            createPropertyLValue(ktLeft, descriptor, propertyReceiver, getTypeArguments(resolvedCall), origin, superQualifier)
        }

    private fun createPropertyLValue(
        ktExpression: KtExpression,
        descriptor: PropertyDescriptor,
        propertyReceiver: CallReceiver,
        typeArguments: Map<TypeParameterDescriptor, KotlinType>?,
        origin: IrStatementOrigin?,
        superQualifier: ClassDescriptor?
    ): PropertyLValueBase {
        val superQualifierSymbol = superQualifier?.let { context.symbolTable.referenceClass(it) }

        val getterDescriptor = descriptor.getter
        val getterSymbol = getterDescriptor?.let { context.symbolTable.referenceFunction(it.original) }

        val setterDescriptor = descriptor.setter
        val setterSymbol = setterDescriptor?.let { context.symbolTable.referenceFunction(it.original) }

        return if (getterSymbol != null || setterSymbol != null) {
            AccessorPropertyLValue(
                scope,
                ktExpression.startOffset, ktExpression.endOffset, origin,
                descriptor.type,
                getterSymbol,
                getterDescriptor,
                setterSymbol,
                setterDescriptor,
                typeArguments,
                propertyReceiver,
                superQualifierSymbol
            )
        } else
            FieldPropertyLValue(
                scope,
                ktExpression.startOffset, ktExpression.endOffset, origin,
                context.symbolTable.referenceField(descriptor),
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
        val irArray = statementGenerator.generateExpression(ktLeft.arrayExpression!!)
        val irIndexExpressions = ktLeft.indexExpressions.map { statementGenerator.generateExpression(it) }

        val indexedGetResolvedCall = get(BindingContext.INDEXED_LVALUE_GET, ktLeft)
        val indexedGetCall = indexedGetResolvedCall?.let { statementGenerator.pregenerateCallReceivers(it) }

        val indexedSetResolvedCall = get(BindingContext.INDEXED_LVALUE_SET, ktLeft)
        val indexedSetCall = indexedSetResolvedCall?.let { statementGenerator.pregenerateCallReceivers(it) }

        return ArrayAccessAssignmentReceiver(
            irArray, irIndexExpressions, indexedGetCall, indexedSetCall,
            CallGenerator(statementGenerator),
            ktLeft.startOffset, ktLeft.endOffset, origin
        )
    }

}
