/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import org.jetbrains.kotlin.fir.types.*

abstract class LogicSystem(protected val context: ConeInferenceContext) {
    abstract fun joinFlow(flows: Collection<PersistentFlow>, union: Boolean): MutableFlow

    // Returns all known information about the variable, or null if unchanged by this statement:
    abstract fun addTypeStatement(flow: MutableFlow, statement: TypeStatement): TypeStatement?
    abstract fun addImplication(flow: MutableFlow, implication: Implication)
    abstract fun addLocalVariableAlias(flow: MutableFlow, alias: RealVariable, underlyingVariable: RealVariable)
    abstract fun recordNewAssignment(flow: MutableFlow, variable: RealVariable, index: Int)
    abstract fun isSameValueIn(a: PersistentFlow, b: MutableFlow, variable: RealVariable): Boolean
    abstract fun isSameValueIn(a: PersistentFlow, b: PersistentFlow, variable: RealVariable): Boolean

    fun addTypeStatements(flow: MutableFlow, statements: TypeStatements): List<TypeStatement> =
        statements.values.mapNotNull { addTypeStatement(flow, it) }

    abstract fun translateVariableFromConditionInStatements(
        flow: MutableFlow,
        originalVariable: DataFlowVariable,
        newVariable: DataFlowVariable,
        transform: (Implication) -> Implication? = { it },
    )

    abstract fun approveOperationStatement(flow: PersistentFlow, statement: OperationStatement): TypeStatements
    abstract fun approveOperationStatement(
        flow: MutableFlow,
        statement: OperationStatement,
        removeApprovedOrImpossible: Boolean,
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
                this[variable] = this[variable]?.let { and(listOf(it, rightStatement))!! } ?: rightStatement
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
