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

import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi2ir.defaultLoad
import org.jetbrains.kotlin.psi2ir.intermediate.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.isSafeCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.scopes.receivers.ThisClassReceiver
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import org.jetbrains.kotlin.types.typeUtil.makeNullable
import java.lang.AssertionError


class OperatorExpressionGenerator(
        val statementGenerator: StatementGenerator
) : GeneratorWithScope {
    override val scope: Scope get() = statementGenerator.scope 
    override val context: GeneratorContext get() = statementGenerator.context

    fun generatePrefixExpression(expression: KtPrefixExpression): IrExpression {
        val ktOperator = expression.operationReference.getReferencedNameElementType()
        val irOperator = getPrefixOperator(ktOperator)

        return when (irOperator) {
            null -> throw AssertionError("Unexpected prefix operator: $ktOperator")
            in INCREMENT_DECREMENT_OPERATORS -> generatePrefixIncrementDecrementOperator(expression, irOperator)
            in OPERATORS_DESUGARED_TO_CALLS -> generatePrefixOperatorAsCall(expression, irOperator)
            else -> createDummyExpression(expression, ktOperator.toString())
        }
    }

    fun generatePostfixExpression(expression: KtPostfixExpression): IrExpression {
        val ktOperator = expression.operationReference.getReferencedNameElementType()
        val irOperator = getPostfixOperator(ktOperator)

        return when (irOperator) {
            null -> throw AssertionError("Unexpected postfix operator: $ktOperator")
            in INCREMENT_DECREMENT_OPERATORS -> generatePostfixIncrementDecrementOperator(expression, irOperator)
            IrOperator.EXCLEXCL -> generateExclExclOperator(expression, irOperator)
            else -> createDummyExpression(expression, ktOperator.toString())
        }
    }

    fun generateCastExpression(expression: KtBinaryExpressionWithTypeRHS): IrExpression {
        val ktOperator = expression.operationReference.getReferencedNameElementType()
        val irOperator = getIrTypeOperator(ktOperator)
        val rhsType = getOrFail(BindingContext.TYPE, expression.right!!)

        val resultType = when (irOperator) {
            IrTypeOperator.CAST ->
                rhsType
            IrTypeOperator.SAFE_CAST ->
                rhsType.makeNullable()
            else ->
                throw AssertionError("Unexpected IrTypeOperator: $irOperator")
        }

        return IrTypeOperatorCallImpl(expression.startOffset, expression.endOffset, resultType, irOperator, rhsType,
                                      statementGenerator.generateExpression(expression.left))
    }

    fun generateInstanceOfExpression(expression: KtIsExpression): IrStatement {
        val ktOperator = expression.operationReference.getReferencedNameElementType()
        val irOperator = getIrTypeOperator(ktOperator)!!
        val againstType = getOrFail(BindingContext.TYPE, expression.typeReference)

        return IrTypeOperatorCallImpl(expression.startOffset, expression.endOffset, context.builtIns.booleanType, irOperator,
                                      againstType, statementGenerator.generateExpression(expression.leftHandSide))
    }

    fun generateBinaryExpression(expression: KtBinaryExpression): IrExpression {
        val ktOperator = expression.operationReference.getReferencedNameElementType()
        if (ktOperator == KtTokens.IDENTIFIER) {
            return generateBinaryOperatorAsCall(expression, null)
        }

        val irOperator = getInfixOperator(ktOperator)

        return when (irOperator) {
            null -> throw AssertionError("Unexpected infix operator: $ktOperator")
            IrOperator.EQ -> generateAssignment(expression)
            IrOperator.ELVIS -> generateElvis(expression)
            in AUGMENTED_ASSIGNMENTS -> generateAugmentedAssignment(expression, irOperator)
            in OPERATORS_DESUGARED_TO_CALLS -> generateBinaryOperatorAsCall(expression, irOperator)
            in COMPARISON_OPERATORS -> generateComparisonOperator(expression, irOperator)
            in EQUALITY_OPERATORS -> generateEqualityOperator(expression, irOperator)
            in IDENTITY_OPERATORS -> generateIdentityOperator(expression, irOperator)
            in IN_OPERATORS -> generateInOperator(expression, irOperator)
            in BINARY_BOOLEAN_OPERATORS -> generateBinaryBooleanOperator(expression, irOperator)
            else -> createDummyExpression(expression, ktOperator.toString())
        }
    }

    private fun generateElvis(expression: KtBinaryExpression): IrExpression {
        val specialCallForElvis = getResolvedCall(expression)!!
        val returnType = specialCallForElvis.resultingDescriptor.returnType!!
        val irArgument0 = statementGenerator.generateExpression(expression.left!!)
        val irArgument1 = statementGenerator.generateExpression(expression.right!!)

        val irBlock = IrBlockImpl(expression.startOffset, expression.endOffset, returnType, IrOperator.ELVIS)
        val irArgument0Value = createRematerializableOrTemporary(scope, irArgument0, irBlock, "elvis_lhs")
        irBlock.addStatement(IrIfThenElseImpl(
                expression.startOffset, expression.endOffset, returnType,
                context.equalsNull(expression.startOffset, expression.endOffset, irArgument0Value.load()),
                irArgument1,
                irArgument0Value.load()
        ))
        return irBlock
    }

    private fun generateBinaryBooleanOperator(expression: KtBinaryExpression, irOperator: IrOperator): IrExpression {
        val irArgument0 = statementGenerator.generateExpression(expression.left!!)
        val irArgument1 = statementGenerator.generateExpression(expression.right!!)
        return when (irOperator) {
            IrOperator.OROR ->
                context.oror(expression.startOffset, expression.endOffset, irArgument0, irArgument1)
            IrOperator.ANDAND ->
                context.andand(expression.startOffset, expression.endOffset, irArgument0, irArgument1)
            else ->
                throw AssertionError("Unexpected binary boolean operator $irOperator")
        }
    }

    private fun generateInOperator(expression: KtBinaryExpression, irOperator: IrOperator): IrExpression {
        val containsCall = getResolvedCall(expression)!!

        val irContainsCall = CallGenerator(this).generateCall(expression, statementGenerator.pregenerateCall(containsCall), irOperator)

        return when (irOperator) {
            IrOperator.IN ->
                irContainsCall
            IrOperator.NOT_IN ->
                IrUnaryPrimitiveImpl(expression.startOffset, expression.endOffset, IrOperator.NOT_IN, context.irBuiltIns.booleanNot,
                                     irContainsCall)
            else ->
                throw AssertionError("Unexpected in-operator $irOperator")
        }

    }

    private fun generateIdentityOperator(expression: KtBinaryExpression, irOperator: IrOperator): IrExpression {
        val irArgument0 = statementGenerator.generateExpression(expression.left!!)
        val irArgument1 = statementGenerator.generateExpression(expression.right!!)


        val irIdentityEquals = IrBinaryPrimitiveImpl(expression.startOffset, expression.endOffset, irOperator, context.irBuiltIns.eqeqeq,
                                                     irArgument0, irArgument1)

        return when (irOperator) {
            IrOperator.EQEQEQ ->
                irIdentityEquals
            IrOperator.EXCLEQEQ ->
                IrUnaryPrimitiveImpl(expression.startOffset, expression.endOffset, IrOperator.EXCLEQEQ, context.irBuiltIns.booleanNot,
                                     irIdentityEquals)
            else ->
                throw AssertionError("Unexpected identity operator $irOperator")
        }

    }

    private fun generateEqualityOperator(expression: KtBinaryExpression, irOperator: IrOperator): IrExpression {
        val irArgument0 = statementGenerator.generateExpression(expression.left!!)
        val irArgument1 = statementGenerator.generateExpression(expression.right!!)

        val irEquals = IrBinaryPrimitiveImpl(expression.startOffset, expression.endOffset,
                                             irOperator, context.irBuiltIns.eqeq, irArgument0, irArgument1)

        return when (irOperator) {
            IrOperator.EQEQ ->
                irEquals
            IrOperator.EXCLEQ ->
                IrUnaryPrimitiveImpl(expression.startOffset, expression.endOffset, IrOperator.EXCLEQ,
                                     context.irBuiltIns.booleanNot, irEquals)
            else ->
                throw AssertionError("Unexpected equality operator $irOperator")
        }

    }

    private fun generateComparisonOperator(expression: KtBinaryExpression, irOperator: IrOperator): IrExpression {
        val compareToCall = getResolvedCall(expression)!!

        val irCompareToCall = CallGenerator(this).generateCall(expression, statementGenerator.pregenerateCall(compareToCall), irOperator)

        val compareToZeroDescriptor = when (irOperator) {
            IrOperator.LT -> context.irBuiltIns.lt0
            IrOperator.LTEQ -> context.irBuiltIns.lteq0
            IrOperator.GT -> context.irBuiltIns.gt0
            IrOperator.GTEQ -> context.irBuiltIns.gteq0
            else -> throw AssertionError("Unexpected comparison operator: $irOperator")
        }

        return IrUnaryPrimitiveImpl(expression.startOffset, expression.endOffset, irOperator, compareToZeroDescriptor, irCompareToCall)
    }


    private fun generateBinaryOperatorAsCall(expression: KtBinaryExpression, irOperator: IrOperator?): IrExpression {
        val operatorCall = getResolvedCall(expression)!!
        return CallGenerator(this).generateCall(expression, statementGenerator.pregenerateCall(operatorCall), irOperator)
    }

    private fun generatePrefixIncrementDecrementOperator(expression: KtPrefixExpression, irOperator: IrOperator): IrExpression {
        val opResolvedCall = getResolvedCall(expression)!!
        val ktBaseExpression = expression.baseExpression!!
        val irAssignmentReceiver = generateAssignmentReceiver(ktBaseExpression, irOperator)

        return irAssignmentReceiver.assign { irLValue ->
            val irBlock = IrBlockImpl(expression.startOffset, expression.endOffset, irLValue.type, irOperator)

            // VAR tmp = [lhs].inc()
            val opCall = statementGenerator.pregenerateCall(opResolvedCall)
            opCall.setExplicitReceiverValue(irLValue)
            val irOpCall = CallGenerator(this).generateCall(expression, opCall, irOperator)
            val irTmp = statementGenerator.scope.createTemporaryVariable(irOpCall)
            irBlock.addStatement(irTmp)

            // [lhs] = tmp
            irBlock.addStatement(irLValue.store(irTmp.defaultLoad()))

            // ^ tmp
            irBlock.addStatement(irTmp.defaultLoad())

            irBlock
        }
    }

    private fun generatePostfixIncrementDecrementOperator(expression: KtPostfixExpression, irOperator: IrOperator): IrExpression {
        val opResolvedCall = getResolvedCall(expression)!!
        val ktBaseExpression = expression.baseExpression!!
        val irAssignmentReceiver = generateAssignmentReceiver(ktBaseExpression, irOperator)

        return irAssignmentReceiver.assign { irLValue ->
            val irBlock = IrBlockImpl(expression.startOffset, expression.endOffset, irLValue.type, irOperator)

            // VAR tmp = [lhs]
            val irTmp = scope.createTemporaryVariable(irLValue.load())
            irBlock.addStatement(irTmp)

            // [lhs] = tmp.inc()
            val opCall = statementGenerator.pregenerateCall(opResolvedCall)
            opCall.setExplicitReceiverValue(VariableLValue(irTmp))
            val irOpCall = CallGenerator(this).generateCall(expression, opCall, irOperator)
            irBlock.addStatement(irLValue.store(irOpCall))

            // ^ tmp
            irBlock.addStatement(irTmp.defaultLoad())

            irBlock
        }
    }

    private fun generateExclExclOperator(expression: KtPostfixExpression, irOperator: IrOperator): IrExpression {
        val ktArgument = expression.baseExpression!!
        val irArgument = statementGenerator.generateExpression(ktArgument)
        val ktOperator = expression.operationReference

        val resultType = irArgument.type.makeNotNullable()
        val irBlock = IrBlockImpl(ktOperator.startOffset, ktOperator.endOffset, resultType, irOperator)
        val argumentValue = createRematerializableOrTemporary(scope, irArgument, irBlock, "notnull")
        val irIfThenElse = IrIfThenElseImpl(ktOperator.startOffset, ktOperator.endOffset, resultType,
                                            context.equalsNull(ktOperator.startOffset, ktOperator.endOffset, argumentValue.load()),
                                            context.throwNpe(ktOperator.startOffset, ktOperator.endOffset, irOperator),
                                            argumentValue.load())

        return if (irBlock.statements.isEmpty()) {
            irIfThenElse
        }
        else {
            irBlock.addStatement(irIfThenElse)
            irBlock
        }
    }

    private fun generatePrefixOperatorAsCall(expression: KtPrefixExpression, irOperator: IrOperator): IrExpression {
        val resolvedCall = getResolvedCall(expression)!!
        return CallGenerator(statementGenerator).generateCall(expression, statementGenerator.pregenerateCall(resolvedCall), irOperator)
    }

    private fun generateAugmentedAssignment(expression: KtBinaryExpression, irOperator: IrOperator): IrExpression {
        val opResolvedCall = getResolvedCall(expression)!!
        val isSimpleAssignment = get(BindingContext.VARIABLE_REASSIGNMENT, expression) ?: false
        val ktLeft = expression.left!!
        val ktRight = expression.right!!
        val irAssignmentReceiver = generateAssignmentReceiver(ktLeft, irOperator)

        return irAssignmentReceiver.assign { irLValue ->
            val opCall = statementGenerator.pregenerateCall(opResolvedCall)
            opCall.setExplicitReceiverValue(irLValue)
            opCall.irValueArgumentsByIndex[0] = statementGenerator.generateExpression(ktRight)
            val irOpCall = CallGenerator(this).generateCall(expression, opCall, irOperator)

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

    private fun generateAssignment(expression: KtBinaryExpression): IrExpression {
        val ktLeft = expression.left!!
        val irRhs = statementGenerator.generateExpression(expression.right!!)
        val irAssignmentReceiver = generateAssignmentReceiver(ktLeft, IrOperator.EQ)
        return irAssignmentReceiver.assign(irRhs)
    }

    private fun generateAssignmentReceiver(ktLeft: KtExpression, irOperator: IrOperator): AssignmentReceiver {
        if (ktLeft is KtArrayAccessExpression) {
            return generateArrayAccessAssignmentReceiver(ktLeft, irOperator)
        }

        val resolvedCall = getResolvedCall(ktLeft) ?: TODO("no resolved call for LHS")
        val descriptor = resolvedCall.candidateDescriptor

        return when (descriptor) {
            is LocalVariableDescriptor ->
                if (descriptor.isDelegated)
                    TODO("Delegated local variable")
                else
                    VariableLValue(ktLeft.startOffset, ktLeft.endOffset, descriptor, irOperator)
            is PropertyDescriptor ->
                generateAssignmentReceiverForProperty(descriptor, irOperator, ktLeft, resolvedCall)
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
        if (isPropertyInitializationWithinPrimaryConstructor(descriptor, resolvedCall)) {
            return PropertyInitializerLValue(ktLeft.startOffset, ktLeft.endOffset, descriptor)
        }

        val propertyReceiver = statementGenerator.generateCallReceiver(
                ktLeft, resolvedCall.dispatchReceiver, resolvedCall.extensionReceiver, resolvedCall.call.isSafeCall())

        return SimplePropertyLValue(scope, ktLeft.startOffset, ktLeft.endOffset, irOperator, descriptor, propertyReceiver)
    }

    private fun isPropertyInitializationWithinPrimaryConstructor(descriptor: PropertyDescriptor, resolvedCall: ResolvedCall<*>): Boolean {
        val scopeOwner = statementGenerator.scopeOwner

        return scopeOwner is ConstructorDescriptor && scopeOwner.isPrimary &&
               descriptor.containingDeclaration == scopeOwner.containingDeclaration &&
               resolvedCall.extensionReceiver == null && resolvedCall.dispatchReceiver is ThisClassReceiver
    }

    private fun generateArrayAccessAssignmentReceiver(ktLeft: KtArrayAccessExpression, irOperator: IrOperator): ArrayAccessAssignmentReceiver {
        val irArray = statementGenerator.generateExpression(ktLeft.arrayExpression!!)
        val irIndexExpressions = ktLeft.indexExpressions.map { statementGenerator.generateExpression(it) }

        val indexedGetResolvedCall = get(BindingContext.INDEXED_LVALUE_GET, ktLeft)
        val indexedGetCall = indexedGetResolvedCall?.let { statementGenerator.pregenerateCallReceivers(it) }

        val indexedSetResolvedCall = get(BindingContext.INDEXED_LVALUE_SET, ktLeft)
        val indexedSetCall = indexedSetResolvedCall?.let { statementGenerator.pregenerateCallReceivers(it) }

        return ArrayAccessAssignmentReceiver(irArray, irIndexExpressions, indexedGetCall, indexedSetCall,
                                             CallGenerator(statementGenerator),
                                             ktLeft.startOffset, ktLeft.endOffset, irOperator)
    }

}
