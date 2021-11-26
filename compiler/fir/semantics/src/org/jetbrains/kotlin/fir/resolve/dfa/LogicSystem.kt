/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import org.jetbrains.kotlin.fir.types.ConeInferenceContext
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.canBeNull
import org.jetbrains.kotlin.fir.types.commonSuperTypeOrNull

abstract class LogicSystem<FLOW : Flow>(protected val context: ConeInferenceContext) {
    // ------------------------------- Flow operations -------------------------------

    abstract fun createEmptyFlow(): FLOW
    abstract fun forkFlow(flow: FLOW): FLOW
    abstract fun joinFlow(flows: Collection<FLOW>): FLOW
    abstract fun unionFlow(flows: Collection<FLOW>): FLOW

    abstract fun addTypeStatement(flow: FLOW, statement: TypeStatement)

    abstract fun addImplication(flow: FLOW, implication: Implication)

    abstract fun removeTypeStatementsAboutVariable(flow: FLOW, variable: RealVariable)
    abstract fun removeLogicStatementsAboutVariable(flow: FLOW, variable: DataFlowVariable)
    abstract fun removeAliasInformationAboutVariable(flow: FLOW, variable: RealVariable)

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

    abstract fun addLocalVariableAlias(flow: FLOW, alias: RealVariable, underlyingVariable: RealVariableAndType)
    abstract fun removeLocalVariableAlias(flow: FLOW, alias: RealVariable)

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

    /**
     * Recursively collects all TypeStatements approved by [approvedStatement] and all predicates
     *   that has been implied by it
     *   TODO: or not recursively?
     */
    fun approveOperationStatement(flow: FLOW, approvedStatement: OperationStatement): Collection<TypeStatement> {
        val statements = getImplicationsWithVariable(flow, approvedStatement.variable)
        return approveOperationStatement(flow, approvedStatement, statements).values
    }

    fun orForTypeStatements(
        left: TypeStatements,
        right: TypeStatements,
    ): MutableTypeStatements {
        if (left.isNullOrEmpty() || right.isNullOrEmpty()) return mutableMapOf()
        val map = mutableMapOf<RealVariable, MutableTypeStatement>()
        for (variable in left.keys.intersect(right.keys)) {
            val leftStatement = left.getValue(variable)
            val rightStatement = right.getValue(variable)
            map[variable] = or(listOf(leftStatement, rightStatement))
        }
        return map
    }

    // ------------------------------- Util functions -------------------------------

    // TODO
    protected fun <E> Collection<Collection<E>>.intersectSets(): Set<E> {
        if (isEmpty()) return emptySet()
        val iterator = iterator()
        val result = LinkedHashSet<E>(iterator.next())
        while (iterator.hasNext()) {
            result.retainAll(iterator.next())
        }
        return result
    }

    private inline fun manipulateTypeStatements(
        statements: Collection<TypeStatement>,
        op: (Collection<Set<ConeKotlinType>>) -> MutableSet<ConeKotlinType>
    ): MutableTypeStatement {
        require(statements.isNotEmpty())
        statements.singleOrNull()?.let { return it as MutableTypeStatement }
        val variable = statements.first().variable
        assert(statements.all { it.variable == variable })
        val exactType = op.invoke(statements.map { it.exactType })
        val exactNotType = op.invoke(statements.map { it.exactNotType })
        return MutableTypeStatement(variable, exactType, exactNotType)
    }

    protected fun or(statements: Collection<TypeStatement>): MutableTypeStatement =
        manipulateTypeStatements(statements, ::orForTypes)

    private fun orForTypes(types: Collection<Set<ConeKotlinType>>): MutableSet<ConeKotlinType> {
        if (types.any { it.isEmpty() }) return mutableSetOf()
        val intersectedTypes = types.map {
            if (it.size > 1) {
                context.intersectTypes(it.toList())
            } else {
                assert(it.size == 1) { "We've already checked each set of types is not empty." }
                it.single()
            }
        }
        val result = mutableSetOf<ConeKotlinType>()
        context.commonSuperTypeOrNull(intersectedTypes)?.let {
            if (it.isAcceptableForSmartcast()) {
                result.add(it)
            } else if (!it.canBeNull) {
                result.add(context.anyType())
            }
            Unit
        }
        return result
    }

    protected fun and(statements: Collection<TypeStatement>): MutableTypeStatement =
        manipulateTypeStatements(statements, ::andForTypes)

    private fun andForTypes(types: Collection<Set<ConeKotlinType>>): MutableSet<ConeKotlinType> {
        return types.flatMapTo(mutableSetOf()) { it }
    }
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
