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
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrOperator
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi2ir.builders.defineTemporary
import org.jetbrains.kotlin.psi2ir.builders.irBlock
import org.jetbrains.kotlin.psi2ir.builders.irGet
import org.jetbrains.kotlin.psi2ir.intermediate.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.isSafeCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.scopes.receivers.ThisClassReceiver

class AssignmentGenerator(statementGenerator: StatementGenerator) : StatementGeneratorExtension(statementGenerator) {
    fun generateAssignment(expression: KtBinaryExpression): IrExpression {
        val ktLeft = expression.left!!
        val irRhs = statementGenerator.generateExpression(expression.right!!)
        val irAssignmentReceiver = generateAssignmentReceiver(ktLeft, IrOperator.EQ)
        return irAssignmentReceiver.assign(irRhs)
    }

    fun generateAugmentedAssignment(expression: KtBinaryExpression, irOperator: IrOperator): IrExpression {
        val opResolvedCall = getResolvedCall(expression)!!
        val isSimpleAssignment = get(BindingContext.VARIABLE_REASSIGNMENT, expression) ?: false
        val ktLeft = expression.left!!
        val ktRight = expression.right!!
        val irAssignmentReceiver = generateAssignmentReceiver(ktLeft, irOperator)

        return irAssignmentReceiver.assign { irLValue ->
            val opCall = statementGenerator.pregenerateCall(opResolvedCall)
            opCall.setExplicitReceiverValue(irLValue)
            opCall.irValueArgumentsByIndex[0] = statementGenerator.generateExpression(ktRight)
            val irOpCall = CallGenerator(statementGenerator).generateCall(expression, opCall, irOperator)

            if (isSimpleAssignment) {
                // Set( Op( Get(), RHS ) )
                irLValue.store(irOpCall)
            }
            else {
                // Op( Get(), RHS )
                irOpCall
            }
        }
    }

    fun generatePrefixIncrementDecrement(expression: KtPrefixExpression, irOperator: IrOperator): IrExpression {
        val opResolvedCall = getResolvedCall(expression)!!
        val ktBaseExpression = expression.baseExpression!!
        val irAssignmentReceiver = generateAssignmentReceiver(ktBaseExpression, irOperator)

        return irAssignmentReceiver.assign { irLValue ->
            irBlock(expression, irOperator, irLValue.type) {
                val opCall = statementGenerator.pregenerateCall(opResolvedCall)
                opCall.setExplicitReceiverValue(irLValue)
                val irOpCall = CallGenerator(statementGenerator).generateCall(expression, opCall, irOperator)
                val temporary = defineTemporary(irOpCall)
                +irLValue.store(irGet(temporary))
                +irGet(temporary)
            }
        }
    }

    fun generatePostfixIncrementDecrement(expression: KtPostfixExpression, irOperator: IrOperator): IrExpression {
        val opResolvedCall = getResolvedCall(expression)!!
        val ktBaseExpression = expression.baseExpression!!
        val irAssignmentReceiver = generateAssignmentReceiver(ktBaseExpression, irOperator)

        return irAssignmentReceiver.assign { irLValue ->
            irBlock(expression, irOperator, irLValue.type) {
                val temporary = defineTemporary(irLValue.load())
                val opCall = statementGenerator.pregenerateCall(opResolvedCall)
                opCall.setExplicitReceiverValue(VariableLValue(startOffset, endOffset, temporary))
                val irOpCall = CallGenerator(statementGenerator).generateCall(expression, opCall, irOperator)
                +irLValue.store(irOpCall)
                +irGet(temporary)
            }
        }
    }

    fun generateAssignmentReceiver(ktLeft: KtExpression, operator: IrOperator): AssignmentReceiver {
        if (ktLeft is KtArrayAccessExpression) {
            return generateArrayAccessAssignmentReceiver(ktLeft, operator)
        }

        val resolvedCall = getResolvedCall(ktLeft) ?: TODO("no resolved call for LHS")
        val descriptor = resolvedCall.candidateDescriptor

        return when (descriptor) {
            is SyntheticFieldDescriptor ->
                BackingFieldLValue(ktLeft.startOffset, ktLeft.endOffset, descriptor.propertyDescriptor, operator)
            is LocalVariableDescriptor ->
                if (descriptor.isDelegated)
                    DelegatedLocalPropertyLValue(ktLeft.startOffset, ktLeft.endOffset, descriptor, operator)
                else
                    VariableLValue(ktLeft.startOffset, ktLeft.endOffset, descriptor, operator)
            is PropertyDescriptor ->
                generateAssignmentReceiverForProperty(descriptor, operator, ktLeft, resolvedCall)
            is VariableDescriptor ->
                VariableLValue(ktLeft.startOffset, ktLeft.endOffset, descriptor, operator)
            else ->
                TODO("Other cases of LHS")
        }
    }

    private fun generateAssignmentReceiverForProperty(
            descriptor: PropertyDescriptor,
            irOperator: IrOperator,
            ktLeft: KtExpression,
            resolvedCall: ResolvedCall<*>
    ): AssignmentReceiver {
        if (isValInitializationInConstructor(descriptor, resolvedCall)) {
            return BackingFieldLValue(ktLeft.startOffset, ktLeft.endOffset, descriptor, null)
        }

        val propertyReceiver = statementGenerator.generateCallReceiver(
                ktLeft, resolvedCall.dispatchReceiver, resolvedCall.extensionReceiver, resolvedCall.call.isSafeCall())

        val superQualifier = getSuperQualifier(resolvedCall)

        return SimplePropertyLValue(context, scope, ktLeft.startOffset, ktLeft.endOffset, irOperator, descriptor,
                                    propertyReceiver, superQualifier)
    }

    private fun isValInitializationInConstructor(descriptor: PropertyDescriptor, resolvedCall: ResolvedCall<*>): Boolean =
            !descriptor.isVar &&
            statementGenerator.scopeOwner.let { it is ConstructorDescriptor || it is ClassDescriptor } &&
            resolvedCall.dispatchReceiver is ThisClassReceiver

    private fun generateArrayAccessAssignmentReceiver(ktLeft: KtArrayAccessExpression, irOperator: IrOperator): ArrayAccessAssignmentReceiver {
        val irArray = statementGenerator.generateExpression(ktLeft.arrayExpression!!)
        val irIndexExpressions = ktLeft.indexExpressions.map { statementGenerator.generateExpression(it) }

        val indexedGetResolvedCall = get(BindingContext.INDEXED_LVALUE_GET, ktLeft)
        val indexedGetCall = indexedGetResolvedCall?.let { statementGenerator.pregenerateCallWithReceivers(it) }

        val indexedSetResolvedCall = get(BindingContext.INDEXED_LVALUE_SET, ktLeft)
        val indexedSetCall = indexedSetResolvedCall?.let { statementGenerator.pregenerateCallWithReceivers(it) }

        return ArrayAccessAssignmentReceiver(irArray, irIndexExpressions, indexedGetCall, indexedSetCall,
                                             CallGenerator(statementGenerator),
                                             ktLeft.startOffset, ktLeft.endOffset, irOperator)
    }

}
