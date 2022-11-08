/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import org.jetbrains.kotlin.fir.types.*

abstract class LogicSystem<FLOW : Flow>(protected val context: ConeInferenceContext) {
    // ------------------------------- Flow operations -------------------------------

    abstract fun createEmptyFlow(): FLOW
    abstract fun forkFlow(flow: FLOW): FLOW
    abstract fun joinFlow(flows: Collection<FLOW>): FLOW
    abstract fun unionFlow(flows: Collection<FLOW>): FLOW

    abstract fun addTypeStatement(flow: FLOW, statement: TypeStatement)

    abstract fun addImplication(flow: FLOW, implication: Implication)

    abstract fun removeAllAboutVariable(flow: FLOW, variable: RealVariable)

    abstract fun translateVariableFromConditionInStatements(
        flow: FLOW,
        originalVariable: DataFlowVariable,
        newVariable: DataFlowVariable,
        shouldRemoveOriginalStatements: Boolean,
        filter: (Implication) -> Boolean = { true },
        transform: (Implication) -> Implication? = { it },
    )

    abstract fun approveStatementsInsideFlow(
        flow: FLOW,
        approvedStatement: OperationStatement,
        shouldForkFlow: Boolean,
        shouldRemoveSynthetics: Boolean,
    ): FLOW

    abstract fun addLocalVariableAlias(flow: FLOW, alias: RealVariable, underlyingVariable: RealVariable)

    abstract fun recordNewAssignment(flow: FLOW, variable: RealVariable, index: Int)

    protected abstract fun getImplicationsWithVariable(flow: FLOW, variable: DataFlowVariable): Collection<Implication>

    protected abstract fun ConeKotlinType.isAcceptableForSmartcast(): Boolean

    // ------------------------------- Callbacks for updating implicit receiver stack -------------------------------

    abstract fun processUpdatedReceiverVariable(flow: FLOW, variable: RealVariable)
    abstract fun updateAllReceivers(flow: FLOW)

    // ------------------------------- Public TypeStatement util functions -------------------------------

    data class InfoForBooleanOperator(
        val conditionalFromLeft: Collection<Implication>,
        val conditionalFromRight: Collection<Implication>,
        val knownFromRight: TypeStatements,
    )

    abstract fun collectInfoForBooleanOperator(
        leftFlow: FLOW,
        leftVariable: DataFlowVariable,
        rightFlow: FLOW,
        rightVariable: DataFlowVariable,
    ): InfoForBooleanOperator

    abstract fun approveStatementsTo(
        destination: MutableTypeStatements,
        flow: FLOW,
        approvedStatement: OperationStatement,
        statements: Collection<Implication>,
    )

    fun orForTypeStatements(left: TypeStatements, right: TypeStatements): MutableTypeStatements {
        if (left.isEmpty() || right.isEmpty()) return mutableMapOf()
        val map = mutableMapOf<RealVariable, MutableTypeStatement>()
        for (variable in left.keys.intersect(right.keys)) {
            val leftStatement = left.getValue(variable)
            val rightStatement = right.getValue(variable)
            map[variable] = or(listOf(leftStatement, rightStatement))
        }
        return map
    }

    fun andForTypeStatements(left: TypeStatements, right: TypeStatements): MutableTypeStatements {
        if (left.isEmpty() && right.isEmpty()) return mutableMapOf()
        val map = left.asMutableStatements()
        for ((variable, rightStatement) in right) {
            map[variable] = and(listOfNotNull(map[variable], rightStatement))
        }
        return map
    }

    // ------------------------------- Util functions -------------------------------

    private fun foldStatements(statements: Collection<TypeStatement>, all: Boolean): MutableTypeStatement {
        require(statements.isNotEmpty())
        statements.singleOrNull()?.let { return it.asMutableStatement() }
        val variable = statements.first().variable
        assert(statements.all { it.variable == variable })
        // TypeStatement(variable, exactType, exactNotType) =
        //   variable is intersect(exactType) && variable !is intersect(exactNotType)
        // So `and` of two type statements computes `and` of exactType and `or` of `exactNotType`,
        // while `or` is the opposite.
        return if (all) {
            val exactType = statements.flatMapTo(mutableSetOf()) { it.exactType }
            // variable !is a && variable !is b =/=> variable !is commonSuperType(a, b)
            // So in this case we can only take the union if either type is a subtype of the other.
            val exactNotType = unifyTypes(statements.map { it.exactNotType }, onlyInputTypes = true)
            MutableTypeStatement(variable, exactType, exactNotType?.let { mutableSetOf(it) } ?: mutableSetOf())
        } else {
            val exactType = unifyTypes(statements.map { it.exactType }, onlyInputTypes = false)
            val exactNotType = statements.flatMapTo(mutableSetOf()) { it.exactNotType }
            MutableTypeStatement(variable, exactType?.let { mutableSetOf(it) } ?: mutableSetOf(), exactNotType)
        }
    }

    private fun unifyTypes(types: Collection<Set<ConeKotlinType>>, onlyInputTypes: Boolean): ConeKotlinType? {
        if (types.any { it.isEmpty() }) return null
        val intersected = types.map { ConeTypeIntersector.intersectTypes(context, it.toList()) }
        val unified = context.commonSuperTypeOrNull(intersected) ?: return null
        return when {
            unified.isAcceptableForSmartcast() -> unified
            unified.canBeNull -> null
            else -> context.anyType()
        }.takeIf { !onlyInputTypes || it in intersected }
    }

    protected fun and(statements: Collection<TypeStatement>): MutableTypeStatement =
        foldStatements(statements, all = true)

    protected fun or(statements: Collection<TypeStatement>): MutableTypeStatement =
        foldStatements(statements, all = false)
}

fun <FLOW : Flow> LogicSystem<FLOW>.approveOperationStatement(
    flow: FLOW,
    approvedStatement: OperationStatement,
    statements: Collection<Implication>,
): MutableTypeStatements {
    return mutableMapOf<RealVariable, MutableTypeStatement>().apply {
        approveStatementsTo(this, flow, approvedStatement, statements)
    }
}

/*
 *  used for:
 *   1. val b = x is String
 *   2. b = x is String
 *   3. !b | b.not()   for Booleans
 */
fun <F : Flow> LogicSystem<F>.replaceVariableFromConditionInStatements(
    flow: F,
    originalVariable: DataFlowVariable,
    newVariable: DataFlowVariable,
    filter: (Implication) -> Boolean = { true },
    transform: (Implication) -> Implication = { it },
) {
    translateVariableFromConditionInStatements(
        flow,
        originalVariable,
        newVariable,
        shouldRemoveOriginalStatements = true,
        filter,
        transform,
    )
}
