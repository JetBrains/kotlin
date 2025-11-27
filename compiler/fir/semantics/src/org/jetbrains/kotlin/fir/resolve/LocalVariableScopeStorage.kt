/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import org.jetbrains.kotlin.fir.SessionAndScopeSessionHolder
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.calls.ExpressionReceiverValue
import org.jetbrains.kotlin.fir.resolve.dfa.DataFlowVariable
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType

/**
 * Component that caches scopes for local variable symbols.
 *
 * Reusing scopes prevents the repeated capturing of projections which fixes some false-positive type mismatches like in the following
 * example:
 *
 * ```kt
 * fun test(a: Array<*>) {
 *     a.set(0, a.get(0))
 * }
 * ```
 */
class LocalVariableScopeStorage private constructor(
    private val map: PersistentMap<FirVariableSymbol<*>, MutableMap<Pair<DataFlowVariable, ConeKotlinType>, FirTypeScope?>>,
) {
    constructor() : this(persistentMapOf())

    fun addLocalVariable(symbol: FirVariableSymbol<*>): LocalVariableScopeStorage {
        return LocalVariableScopeStorage(map.put(symbol, mutableMapOf()))
    }

    context(c: SessionAndScopeSessionHolder)
    fun getScope(
        receiverValue: ExpressionReceiverValue,
        getDataFlowVariable: () -> DataFlowVariable?,
    ): FirTypeScope? {
        val symbol = receiverValue.receiverExpression.toResolvedCallableSymbol(c.session) as? FirVariableSymbol
            ?: return receiverValue.scope()
        val map = map[symbol]
            ?: return receiverValue.scope()
        val dataFlowVariable = getDataFlowVariable()
            ?: return receiverValue.scope()

        return map.getOrPut(dataFlowVariable to receiverValue.type) { receiverValue.scope() }
    }
}
