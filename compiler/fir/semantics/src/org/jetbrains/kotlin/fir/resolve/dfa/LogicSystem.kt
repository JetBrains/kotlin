/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import org.jetbrains.kotlin.fir.types.*

abstract class LogicSystem<FLOW : Flow>(protected val context: ConeInferenceContext) {
    // --------------------------- Flow graph constructors ---------------------------
    abstract fun createEmptyFlow(): FLOW
    abstract fun forkFlow(flow: FLOW): FLOW
    abstract fun joinFlow(flows: Collection<FLOW>): FLOW
    abstract fun unionFlow(flows: Collection<FLOW>): FLOW

    // -------------------------------- Flow mutators --------------------------------
    // Returns all known information about the variable, or null if unchanged by this statement:
    abstract fun addTypeStatement(flow: FLOW, statement: TypeStatement): TypeStatement?
    abstract fun addTypeStatements(flow: FLOW, statements: TypeStatements): List<TypeStatement>
    abstract fun addImplication(flow: FLOW, implication: Implication)
    abstract fun addLocalVariableAlias(flow: FLOW, alias: RealVariable, underlyingVariable: RealVariable)
    abstract fun recordNewAssignment(flow: FLOW, variable: RealVariable, index: Int)
    abstract fun isSameValueIn(a: FLOW, b: FLOW, variable: RealVariable): Boolean

    abstract fun translateVariableFromConditionInStatements(
        flow: FLOW,
        originalVariable: DataFlowVariable,
        newVariable: DataFlowVariable,
        shouldRemoveOriginalStatements: Boolean = originalVariable.isSynthetic(),
        transform: (Implication) -> Implication? = { it },
    )

    // This does *not* commit the results to the flow (but it does mutate the flow if removeApprovedOrImpossible=true)
    abstract fun approveOperationStatement(
        flow: FLOW,
        approvedStatement: OperationStatement,
        removeApprovedOrImpossible: Boolean = false
    ): TypeStatements

    protected abstract fun ConeKotlinType.isAcceptableForSmartcast(): Boolean

    // ------------------------------- Public TypeStatement util functions -------------------------------

    fun orForTypeStatements(left: TypeStatements, right: TypeStatements): TypeStatements = when {
        left.isEmpty() -> left
        right.isEmpty() -> right
        else -> buildMap {
            for ((variable, leftStatement) in left) {
                put(variable, or(listOf(leftStatement, right[variable] ?: continue))!!)
            }
        }
    }

    fun andForTypeStatements(left: TypeStatements, right: TypeStatements): TypeStatements = when {
        left.isEmpty() -> right
        right.isEmpty() -> left
        else -> left.toMutableMap().apply {
            for ((variable, rightStatement) in right) {
                put(variable, { rightStatement }, { and(listOf(it, rightStatement))!! })
            }
        }
    }

    private inline fun Collection<TypeStatement>.singleOrNew(exactType: () -> MutableSet<ConeKotlinType>): TypeStatement? =
        when (size) {
            0 -> null
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
            unified.isNullableAny -> null
            unified.isAcceptableForSmartcast() -> unified
            unified.canBeNull -> null
            else -> context.anyType()
        }
    }

    protected operator fun MutableTypeStatement.plusAssign(other: TypeStatement) {
        exactType += other.exactType
    }

    fun and(statements: Collection<TypeStatement>): TypeStatement? =
        statements.singleOrNew { statements.flatMapTo(mutableSetOf()) { it.exactType } }

    fun or(statements: Collection<TypeStatement>): TypeStatement? =
        statements.singleOrNew { unifyTypes(statements.map { it.exactType })?.let { mutableSetOf(it) } ?: mutableSetOf() }
}
