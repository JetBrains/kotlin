/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSymbolOwner
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.resolve.calls.FirNamedReferenceWithCandidate
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirAccessorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.types.*

abstract class DataFlowResolver<FLOW : Flow> {

    abstract val session: FirSession
    abstract val logicSystem: LogicSystem<FLOW>
    abstract val variableStorage: VariableStorage
    abstract val variablesForWhenConditions: MutableMap<WhenBranchConditionExitNode, DataFlowVariable>

    abstract var CFGNode<*>.flow: FLOW

    private val any: ConeClassLikeType get() = session.builtinTypes.anyType.type
    private val nullableNothing: ConeClassLikeType get() = session.builtinTypes.nullableNothingType.type

    fun processEqualityOperatorCall(node: EqualityOperatorCallNode) {
        val operation = node.fir.operation
        val leftOperand = node.fir.arguments[0]
        val rightOperand = node.fir.arguments[1]

        val leftConst = leftOperand as? FirConstExpression<*>
        val rightConst = rightOperand as? FirConstExpression<*>

        when {
            leftConst != null && rightConst != null -> return
            leftConst?.kind == FirConstKind.Null -> processEqNull(node, rightOperand, operation)
            rightConst?.kind == FirConstKind.Null -> processEqNull(node, leftOperand, operation)
            leftConst != null -> processEqWithConst(node, rightOperand, leftConst, operation)
            rightConst != null -> processEqWithConst(node, leftOperand, rightConst, operation)
            else -> processEq(node, leftOperand, rightOperand, operation)
        }
    }

    private fun processEqWithConst(
        node: EqualityOperatorCallNode, operand: FirExpression, const: FirConstExpression<*>, operation: FirOperation
    ) {
        val isEq = operation.isEq()
        val expressionVariable = variableStorage.createSyntheticVariable(node.fir)
        val flow = node.flow
        val operandVariable = variableStorage.getOrCreateVariable(node.previousFlow, operand)
        // expression == const -> expression != null
        flow.addImplication((expressionVariable eq isEq) implies (operandVariable notEq null))
        if (operandVariable is RealVariable) {
            flow.addImplication((expressionVariable eq isEq) implies (operandVariable typeEq any))
        }

        // propagating facts for (... == true) and (... == false)
        if (const.kind == FirConstKind.Boolean) {
            val constValue = const.value as Boolean
            val shouldInvert = isEq xor constValue

            flow.addImplication((expressionVariable eq true) implies (operandVariable eq !shouldInvert))
            flow.addImplication((expressionVariable eq false) implies (operandVariable eq shouldInvert))

            logicSystem.translateVariableFromConditionInStatements(
                flow,
                operandVariable,
                expressionVariable,
                shouldRemoveOriginalStatements = operandVariable.isSynthetic()
            ) {
                when (it.condition.operation) {
                    Operation.EqNull, Operation.NotEqNull -> {
                        (expressionVariable eq isEq) implies (it.effect)
                    }
                    Operation.EqTrue, Operation.EqFalse -> {
                        if (shouldInvert) (it.condition.invert()) implies (it.effect)
                        else it
                    }
                }
            }
        }
    }

    private fun processEq(
        node: EqualityOperatorCallNode, leftOperand: FirExpression, rightOperand: FirExpression, operation: FirOperation
    ) {
        val leftIsNullable = leftOperand.coneType.isMarkedNullable
        val rightIsNullable = rightOperand.coneType.isMarkedNullable
        // left == right && right not null -> left != null
        when {
            leftIsNullable && rightIsNullable -> return
            leftIsNullable -> processEqNull(node, leftOperand, operation.invert())
            rightIsNullable -> processEqNull(node, rightOperand, operation.invert())
        }

        if (operation == FirOperation.IDENTITY || operation == FirOperation.NOT_IDENTITY) {
            processIdentity(node, leftOperand, rightOperand, operation)
        }
    }

    private fun processEqNull(node: EqualityOperatorCallNode, operand: FirExpression, operation: FirOperation) {
        val flow = node.flow
        val expressionVariable = variableStorage.createSyntheticVariable(node.fir)
        val operandVariable = variableStorage.getOrCreateVariable(node.previousFlow, operand)

        val isEq = operation.isEq()

        val predicate = when (isEq) {
            true -> operandVariable eq null
            false -> operandVariable notEq null
        }

        logicSystem.approveOperationStatement(flow, predicate).forEach { effect ->
            flow.addImplication((expressionVariable eq true) implies effect)
            flow.addImplication((expressionVariable eq false) implies effect.invert())
        }

        flow.addImplication((expressionVariable eq isEq) implies (operandVariable eq null))
        flow.addImplication((expressionVariable notEq isEq) implies (operandVariable notEq null))

        if (operandVariable is RealVariable) {
            flow.addImplication((expressionVariable eq isEq) implies (operandVariable typeNotEq any))
            flow.addImplication((expressionVariable notEq isEq) implies (operandVariable typeEq any))

//            TODO: design do we need casts to Nothing?
//            flow.addImplication((expressionVariable eq !isEq) implies (operandVariable typeEq nullableNothing))
//            flow.addImplication((expressionVariable notEq !isEq) implies (operandVariable typeNotEq nullableNothing))
        }
        node.flow = flow
    }

    private fun processIdentity(
        node: EqualityOperatorCallNode, leftOperand: FirExpression, rightOperand: FirExpression, operation: FirOperation
    ) {
        val flow = node.flow
        val expressionVariable = variableStorage.getOrCreateVariable(node.previousFlow, node.fir)
        val leftOperandVariable = variableStorage.getOrCreateVariable(node.previousFlow, leftOperand)
        val rightOperandVariable = variableStorage.getOrCreateVariable(node.previousFlow, rightOperand)
        val leftOperandType = leftOperand.coneType
        val rightOperandType = rightOperand.coneType
        val isEq = operation.isEq()

        if (leftOperandVariable.isReal()) {
            flow.addImplication((expressionVariable eq isEq) implies (leftOperandVariable typeEq rightOperandType))
            flow.addImplication((expressionVariable notEq isEq) implies (leftOperandVariable typeNotEq rightOperandType))
        }

        if (rightOperandVariable.isReal()) {
            flow.addImplication((expressionVariable eq isEq) implies (rightOperandVariable typeEq leftOperandType))
            flow.addImplication((expressionVariable notEq isEq) implies (rightOperandVariable typeNotEq leftOperandType))
        }

        node.flow = flow
    }

    fun processTypeOperatorCall(node: TypeOperatorCallNode) {
        val typeOperatorCall = node.fir
        if (typeOperatorCall.operation !in FirOperation.TYPES) return
        val type = typeOperatorCall.conversionTypeRef.coneType
        val operandVariable = variableStorage.getOrCreateVariable(node.previousFlow, typeOperatorCall.argument)
        val flow = node.flow

        when (val operation = typeOperatorCall.operation) {
            FirOperation.IS, FirOperation.NOT_IS -> {
                val expressionVariable = variableStorage.createSyntheticVariable(typeOperatorCall)
                val isNotNullCheck = type.nullability == ConeNullability.NOT_NULL
                val isRegularIs = operation == FirOperation.IS
                if (operandVariable.isReal()) {
                    val hasTypeInfo = operandVariable typeEq type
                    val hasNotTypeInfo = operandVariable typeNotEq type

                    fun chooseInfo(trueBranch: Boolean) =
                        if ((typeOperatorCall.operation == FirOperation.IS) == trueBranch) hasTypeInfo else hasNotTypeInfo

                    flow.addImplication((expressionVariable eq true) implies chooseInfo(true))
                    flow.addImplication((expressionVariable eq false) implies chooseInfo(false))

                    if (operation == FirOperation.NOT_IS && type == nullableNothing) {
                        flow.addTypeStatement(operandVariable typeEq any)
                    }
                    if (isNotNullCheck) {
                        flow.addImplication((expressionVariable eq isRegularIs) implies (operandVariable typeEq any))
                        flow.addImplication((expressionVariable eq isRegularIs) implies (operandVariable notEq null))
                    }

                } else {
                    if (isNotNullCheck) {
                        flow.addImplication((expressionVariable eq isRegularIs) implies (operandVariable notEq null))
                    }
                }
            }

            FirOperation.AS -> {
                if (operandVariable.isReal()) {
                    flow.addTypeStatement(operandVariable typeEq type)
                }
                logicSystem.approveStatementsInsideFlow(
                    flow,
                    operandVariable notEq null,
                    shouldRemoveSynthetics = true,
                    shouldForkFlow = false
                )
            }

            FirOperation.SAFE_AS -> {
                val expressionVariable = variableStorage.createSyntheticVariable(typeOperatorCall)
                if (operandVariable.isReal()) {
                    flow.addImplication((expressionVariable notEq null) implies (operandVariable typeEq type))
                    flow.addImplication((expressionVariable eq null) implies (operandVariable typeNotEq type))
                }
                if (type.nullability == ConeNullability.NOT_NULL) {
                    flow.addImplication((expressionVariable notEq null) implies (operandVariable notEq null))
                }
            }

            else -> throw IllegalStateException()
        }
        node.flow = flow
    }

    fun processBinaryBooleanOperator(
        binaryLogicExpression: FirBinaryLogicExpression,
        node: AbstractBinaryExitNode<*>,
        isAnd: Boolean
    ) {
        @Suppress("UnnecessaryVariable")
        val bothEvaluated = isAnd
        val onlyLeftEvaluated = !bothEvaluated

        // Naming for all variables was chosen in assumption that we processing && expression
        val flowFromLeft = node.leftOperandNode.flow
        val flowFromRight = node.rightOperandNode.flow
        val flow = node.flow

        /*
         * TODO: Here we should handle case when one of arguments is dead (e.g. in cases `false && expr` or `true || expr`)
         *  But since conditions with const are rare it can be delayed
         */

        val leftVariable = variableStorage.getOrCreateVariable(flow, binaryLogicExpression.leftOperand)
        val rightVariable = variableStorage.getOrCreateVariable(flow, binaryLogicExpression.rightOperand)
        val operatorVariable = variableStorage.getOrCreateVariable(flow, binaryLogicExpression)

        if (isAnd) {
            flow.addImplication((operatorVariable eq true) implies (leftVariable eq true))
            flow.addImplication((operatorVariable eq true) implies (rightVariable eq true))
        } else {
            flow.addImplication((operatorVariable eq false) implies (leftVariable eq false))
            flow.addImplication((operatorVariable eq false) implies (rightVariable eq false))
        }

        if (!node.leftOperandNode.isDead && node.rightOperandNode.isDead) {
            /*
             * If there was a jump from right argument then we know that we well exit from
             *   boolean operator only if right operand was not executed
             *
             *   a && return => a == false
             *   a || return => a == true
             */
            logicSystem.approveStatementsInsideFlow(
                flow,
                leftVariable eq !isAnd,
                shouldForkFlow = false,
                shouldRemoveSynthetics = true
            )
        } else {
            val (conditionalFromLeft, conditionalFromRight, approvedFromRight) = logicSystem.collectInfoForBooleanOperator(
                flowFromLeft,
                leftVariable,
                flowFromRight,
                rightVariable
            )

            // left && right == True
            // left || right == False
            val approvedIfTrue: MutableTypeStatements = mutableMapOf()
            logicSystem.approveStatementsTo(approvedIfTrue, flowFromRight, leftVariable eq bothEvaluated, conditionalFromLeft)
            logicSystem.approveStatementsTo(approvedIfTrue, flowFromRight, rightVariable eq bothEvaluated, conditionalFromRight)
            approvedFromRight.forEach { (variable, info) ->
                approvedIfTrue.addStatement(variable, info)
            }
            approvedIfTrue.values.forEach { info ->
                flow.addImplication((operatorVariable eq bothEvaluated) implies info)
            }

            // left && right == False
            // left || right == True
            val approvedIfFalse: MutableTypeStatements = mutableMapOf()
            val leftIsFalse = logicSystem.approveOperationStatement(flowFromLeft, leftVariable eq onlyLeftEvaluated, conditionalFromLeft)
            val rightIsFalse =
                logicSystem.approveOperationStatement(flowFromRight, rightVariable eq onlyLeftEvaluated, conditionalFromRight)
            approvedIfFalse.mergeTypeStatements(logicSystem.orForTypeStatements(leftIsFalse, rightIsFalse))
            approvedIfFalse.values.forEach { info ->
                flow.addImplication((operatorVariable eq onlyLeftEvaluated) implies info)
            }
        }

        logicSystem.updateAllReceivers(flow)
        node.flow = flow

        variableStorage.removeSyntheticVariable(leftVariable)
        variableStorage.removeSyntheticVariable(rightVariable)
    }

    fun processCheckNotNullCall(node: CheckNotNullCallNode) {
        val checkNotNullCall: FirCheckNotNullCall = node.fir
        val argument = checkNotNullCall.argument
        variableStorage.getOrCreateRealVariable(node.previousFlow, argument.symbol, argument)?.let { operandVariable ->
            node.flow.addTypeStatement(operandVariable typeEq any)
            logicSystem.approveStatementsInsideFlow(
                node.flow,
                operandVariable notEq null,
                shouldRemoveSynthetics = true,
                shouldForkFlow = false
            )
        }
    }

    fun processInitBlock(node: InitBlockEnterNode, prevNode: CFGNode<*>) {
        node.flow = logicSystem.forkFlow(prevNode.flow)
    }

    fun processPostponedAnonymousFunction(enterNode: PostponedLambdaEnterNode) {
        enterNode.flow = logicSystem.forkFlow(enterNode.flow)
    }

    fun processWhenBranchConditionEnter(node: WhenBranchConditionEnterNode) {
        val previousNode = node.previousNodes.single()
        if (previousNode is WhenBranchConditionExitNode) {
            val conditionVariable = variablesForWhenConditions.remove(previousNode)!!
            node.flow = logicSystem.approveStatementsInsideFlow(
                node.flow,
                conditionVariable eq false,
                shouldForkFlow = true,
                shouldRemoveSynthetics = true
            )
        }
    }

    fun processWhenBranchConditionExit(conditionExitNode: WhenBranchConditionExitNode, branchEnterNode: WhenBranchResultEnterNode) {
        val conditionExitFlow = conditionExitNode.flow
        val conditionVariable = variableStorage.getOrCreateVariable(conditionExitFlow, branchEnterNode.fir.condition)
        variablesForWhenConditions[conditionExitNode] = conditionVariable
        branchEnterNode.flow = logicSystem.approveStatementsInsideFlow(
            conditionExitFlow,
            conditionVariable eq true,
            shouldForkFlow = true,
            shouldRemoveSynthetics = false
        )
    }

    fun processSyntheticElse(syntheticElseNode: WhenSyntheticElseBranchNode, previousConditionExitNode: WhenBranchConditionExitNode) {
        val conditionVariable = variablesForWhenConditions.remove(previousConditionExitNode)!!
        syntheticElseNode.flow = logicSystem.approveStatementsInsideFlow(
            previousConditionExitNode.flow,
            conditionVariable eq false,
            shouldForkFlow = true,
            shouldRemoveSynthetics = true
        )
    }

    fun processLoop(exitNode: LoopExitNode) {
        val singlePreviousNode = exitNode.previousNodes.singleOrNull { !it.isDead }
        if (singlePreviousNode is LoopConditionExitNode) {
            val variable = variableStorage.getOrCreateVariable(exitNode.previousFlow, singlePreviousNode.fir)
            exitNode.flow = logicSystem.approveStatementsInsideFlow(
                exitNode.flow,
                variable eq false,
                shouldForkFlow = false,
                shouldRemoveSynthetics = true
            )
        }
    }

    fun processWhileLoopCondition(loopConditionExitNode: LoopConditionExitNode, loopBlockEnterNode: LoopBlockEnterNode) {
        val conditionExitFlow = loopConditionExitNode.flow
        val loop = loopBlockEnterNode.fir
        loopBlockEnterNode.flow = variableStorage.getVariable(loop.condition, conditionExitFlow)?.let { conditionVariable ->
            logicSystem.approveStatementsInsideFlow(
                conditionExitFlow,
                conditionVariable eq true,
                shouldForkFlow = true,
                shouldRemoveSynthetics = false
            )
        } ?: logicSystem.forkFlow(conditionExitFlow)
    }

    fun processSafeCallEnter(node: EnterSafeCallNode) {
        val safeCall: FirSafeCallExpression = node.fir
        val previousNode = node.firstPreviousNode
        val shouldFork: Boolean
        var flow = if (previousNode is ExitSafeCallNode) {
            shouldFork = false
            previousNode.secondPreviousNode?.flow ?: node.flow
        } else {
            shouldFork = true
            node.flow
        }

        safeCall.receiver.let { receiver ->
            val type = receiver.coneType.takeIf { it.isMarkedNullable }
                ?.withNullability(ConeNullability.NOT_NULL)
                ?: return@let

            val variable = variableStorage.getOrCreateVariable(flow, receiver)
            if (variable is RealVariable) {
                if (shouldFork) {
                    flow = logicSystem.forkFlow(flow)
                }
                flow.addTypeStatement(variable typeEq type)
            }
            flow = logicSystem.approveStatementsInsideFlow(
                flow,
                variable notEq null,
                shouldFork,
                shouldRemoveSynthetics = false
            )
        }

        node.flow = flow
    }

    fun processSafeCallExit(node: ExitSafeCallNode) {
        val safeCall: FirSafeCallExpression = node.fir
        val previousFlow = node.previousFlow

        val variable = variableStorage.getOrCreateVariable(previousFlow, safeCall)
        val receiverVariable = when (variable) {
            // There is some bug with invokes. See KT-36014
            is RealVariable -> variable.explicitReceiverVariable ?: return
            is SyntheticVariable -> variableStorage.getOrCreateVariable(previousFlow, safeCall.receiver)
        }
        logicSystem.addImplication(node.flow, (variable notEq null) implies (receiverVariable notEq null))
        if (receiverVariable.isReal()) {
            logicSystem.addImplication(node.flow, (variable notEq null) implies (receiverVariable typeEq any))
        }
    }

    fun processLeftArgumentOfBinaryBooleanOperator(leftNode: CFGNode<*>, rightNode: CFGNode<*>, isAnd: Boolean) {
        val parentFlow = leftNode.firstPreviousNode.flow
        leftNode.flow = logicSystem.forkFlow(parentFlow)
        val leftOperandVariable = variableStorage.getOrCreateVariable(parentFlow, leftNode.firstPreviousNode.fir)
        rightNode.flow = logicSystem.approveStatementsInsideFlow(
            parentFlow,
            leftOperandVariable eq isAnd,
            shouldForkFlow = true,
            shouldRemoveSynthetics = false
        )
    }

    fun processElvis(lhsExitNode: ElvisLhsExitNode, lhsIsNotNullNode: ElvisLhsIsNotNullNode, rhsEnterNode: ElvisRhsEnterNode) {
        val elvisExpression: FirElvisExpression = lhsExitNode.fir
        val flow = lhsExitNode.flow
        val lhsVariable = variableStorage.getOrCreateVariable(flow, elvisExpression.lhs)
        rhsEnterNode.flow = logicSystem.approveStatementsInsideFlow(
            flow,
            lhsVariable eq null,
            shouldForkFlow = true,
            shouldRemoveSynthetics = false
        )
        lhsIsNotNullNode.flow = logicSystem.approveStatementsInsideFlow(
            flow,
            lhsVariable notEq null,
            shouldForkFlow = true,
            shouldRemoveSynthetics = false
        ).also {
            if (lhsVariable.isReal()) {
                it.addTypeStatement(lhsVariable typeEq any)
            }
        }
    }

    fun processVariableInitialization(
        node: CFGNode<*>,
        initializer: FirExpression,
        property: FirProperty,
        assignment: FirVariableAssignment?
    ) {
        val flow = node.flow
        val propertyVariable = variableStorage.getOrCreateRealVariableWithoutUnwrappingAlias(flow, property.symbol, assignment ?: property)
        val isAssignment = assignment != null
        if (isAssignment) {
            logicSystem.removeLocalVariableAlias(flow, propertyVariable)
            flow.removeAllAboutVariable(propertyVariable)
        }

        variableStorage.getOrCreateRealVariable(flow, initializer.symbol, initializer)?.let { initializerVariable ->
            logicSystem.addLocalVariableAlias(
                flow, propertyVariable,
                RealVariableAndType(initializerVariable, initializer.coneType)
            )
            // node.flow.addImplication((propertyVariable notEq null) implies (initializerVariable notEq null))
        }

        variableStorage.getSyntheticVariable(initializer)?.let { initializerVariable ->
            /*
                 * That part is needed for cases like that:
                 *
                 *   val b = x is String
                 *   ...
                 *   if (b) {
                 *      x.length
                 *   }
                 */
            logicSystem.replaceVariableFromConditionInStatements(flow, initializerVariable, propertyVariable)
        }

        if (isAssignment) {
            if (initializer is FirConstExpression<*> && initializer.kind == FirConstKind.Null) return
            flow.addTypeStatement(propertyVariable typeEq initializer.typeRef.coneType)
        }
    }

    fun processBooleanNot(node: FunctionCallNode) {
        val functionCall: FirFunctionCall = node.fir
        val previousFlow = node.previousFlow
        val booleanExpressionVariable = variableStorage.getOrCreateVariable(previousFlow, node.firstPreviousNode.fir)
        val variable = variableStorage.getOrCreateVariable(previousFlow, functionCall)
        val flow = node.flow

        flow.addImplication((variable eq true) implies (booleanExpressionVariable eq false))
        flow.addImplication((variable eq false) implies (booleanExpressionVariable eq true))

        logicSystem.replaceVariableFromConditionInStatements(
            flow,
            booleanExpressionVariable,
            variable,
            transform = { it.invertCondition() }
        )
    }

    fun processUnionFunctionCallArguments(unionNode: UnionFunctionCallArgumentsNode?) {
        if (unionNode == null) return
        unionNode.flow = logicSystem.unionFlow(unionNode.previousNodes.map { it.flow }).also {
            logicSystem.updateAllReceivers(it)
        }
    }

    fun FirOperation.invert(): FirOperation = when (this) {
        FirOperation.EQ -> FirOperation.NOT_EQ
        FirOperation.NOT_EQ -> FirOperation.EQ
        FirOperation.IDENTITY -> FirOperation.NOT_IDENTITY
        FirOperation.NOT_IDENTITY -> FirOperation.IDENTITY
        else -> throw IllegalArgumentException("$this can not be inverted")
    }

    private val FirExpression.coneType: ConeKotlinType
        get() = typeRef.coneType

    private val FirElement.symbol: AbstractFirBasedSymbol<*>?
        get() = when (this) {
            is FirResolvable -> symbol
            is FirSymbolOwner<*> -> symbol
            is FirWhenSubjectExpression -> whenRef.value.subject?.symbol
            is FirSafeCallExpression -> regularQualifiedAccess.symbol
            else -> null
        }?.takeIf { this is FirThisReceiverExpression || (it !is FirFunctionSymbol<*> && it !is FirAccessorSymbol) }

    private val FirResolvable.symbol: AbstractFirBasedSymbol<*>?
        get() = when (val reference = calleeReference) {
            is FirThisReference -> reference.boundSymbol
            is FirResolvedNamedReference -> reference.resolvedSymbol
            is FirNamedReferenceWithCandidate -> reference.candidateSymbol
            else -> null
        }

    private fun FLOW.addTypeStatement(info: TypeStatement) {
        logicSystem.addTypeStatement(this, info)
    }

    private fun FLOW.addImplication(statement: Implication) {
        logicSystem.addImplication(this, statement)
    }

    private fun FLOW.removeAllAboutVariable(variable: RealVariable?) {
        if (variable == null) return
        logicSystem.removeAllAboutVariable(this, variable)
    }

    private val CFGNode<*>.previousFlow: FLOW
        get() = firstPreviousNode.flow

}