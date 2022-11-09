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

    abstract fun commitOperationStatement(flow: FLOW, statement: OperationStatement, shouldRemoveSynthetics: Boolean)

    abstract fun addLocalVariableAlias(flow: FLOW, alias: RealVariable, underlyingVariable: RealVariable)

    abstract fun recordNewAssignment(flow: FLOW, variable: RealVariable, index: Int)

    abstract fun copyAllInformation(from: FLOW, to: FLOW)

    protected abstract fun getImplicationsWithVariable(flow: FLOW, variable: DataFlowVariable): Collection<Implication>

    protected abstract fun ConeKotlinType.isAcceptableForSmartcast(): Boolean

    // ------------------------------- Callbacks for updating implicit receiver stack -------------------------------

    abstract fun processUpdatedReceiverVariable(flow: FLOW, variable: RealVariable)
    abstract fun updateAllReceivers(flow: FLOW)

    // ------------------------------- Public TypeStatement util functions -------------------------------

    abstract fun approveOperationStatement(flow: FLOW, approvedStatement: OperationStatement): TypeStatements

    fun orForTypeStatements(left: TypeStatements, right: TypeStatements): TypeStatements = when {
        left.isEmpty() -> left
        right.isEmpty() -> right
        else -> buildMap {
            for ((variable, leftStatement) in left) {
                put(variable, or(listOf(leftStatement, right[variable] ?: continue)))
            }
        }
    }

    fun andForTypeStatements(left: TypeStatements, right: TypeStatements): TypeStatements = when {
        left.isEmpty() -> right
        right.isEmpty() -> left
        else -> left.toMutableMap().apply {
            for ((variable, rightStatement) in right) {
                put(variable, { rightStatement }, { and(listOf(it, rightStatement)) })
            }
        }
    }

    private inline fun Collection<TypeStatement>.singleOrNew(exactType: () -> MutableSet<ConeKotlinType>): TypeStatement =
        when (size) {
            0 -> throw AssertionError("need at least one statement")
            1 -> first()
            else -> {
                val variable = first().variable
                assert(all { it.variable == variable }) { "folding statements for different variables" }
                MutableTypeStatement(variable, exactType())
            }
        }

    private fun unifyTypes(types: Collection<Set<ConeKotlinType>>): ConeKotlinType? {
        if (types.any { it.isEmpty() }) return null
        val intersected = types.map { ConeTypeIntersector.intersectTypes(context, it.toList()) }
        val unified = context.commonSuperTypeOrNull(intersected) ?: return null
        return when {
            unified.isAcceptableForSmartcast() -> unified
            unified.canBeNull -> null
            else -> context.anyType()
        }
    }

    protected fun and(statements: Collection<TypeStatement>): TypeStatement =
        statements.singleOrNew { statements.flatMapTo(mutableSetOf()) { it.exactType } }

    protected fun or(statements: Collection<TypeStatement>): TypeStatement =
        statements.singleOrNew { unifyTypes(statements.map { it.exactType })?.let { mutableSetOf(it) } ?: mutableSetOf() }
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
