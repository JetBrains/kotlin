/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isSynthetic
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.scopes.DelicateScopeAPI
import org.jetbrains.kotlin.fir.scopes.FirContainingNamesAwareScope
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

class FirScriptDeclarationsScope(
    val useSiteSession: FirSession,
    val script: FirScript,
) : FirContainingNamesAwareScope() {
    /**
     * This index is lazily calculated as its value might not be used in the Analysis API mode
     */
    private val callablesIndex: Map<Name, List<FirCallableSymbol<*>>> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val result = mutableMapOf<Name, MutableList<FirCallableSymbol<*>>>()
        for (statement in script.declarations) {
            if (statement !is FirCallableDeclaration) continue

            val name = when (statement) {
                is FirVariable -> if (statement.isSynthetic) continue else statement.name
                is FirSimpleFunction -> statement.name
                // TODO: destructuring decl
                else -> continue
            }

            result.getOrPut(name) { mutableListOf() } += statement.symbol
        }

        result
    }

    /**
     * This index is lazily calculated as its value might not be used in the Analysis API mode
     */
    private val classIndex: Map<Name, FirRegularClassSymbol> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val result = mutableMapOf<Name, FirRegularClassSymbol>()
        for (declaration in script.declarations) {
            if (declaration is FirRegularClass) {
                result[declaration.name] = declaration.symbol
            }
        }

        result
    }

    override fun processFunctionsByName(name: Name, out: MutableList<FirNamedFunctionSymbol>) {
        if (name == SpecialNames.INIT) return
        processCallables(name, out)
    }

    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        processCallables(name, processor)
    }

    private inline fun <reified D : FirCallableSymbol<*>> processCallables(
        name: Name,
        processor: (D) -> Unit
    ) {
        val symbols = callablesIndex[name] ?: emptyList()
        for (symbol in symbols) {
            if (symbol is D) {
                processor(symbol)
            }
        }
    }

    private inline fun <reified D : FirCallableSymbol<*>> processCallables(
        name: Name,
        out: MutableList<D>
    ) {
        val symbols = callablesIndex[name] ?: emptyList()
        for (symbol in symbols) {
            if (symbol is D) {
                out.add(symbol)
            }
        }
    }

    override fun getCallableNames(): Set<Name> {
        return callablesIndex.keys
    }

    override fun processClassifiersByNameWithSubstitution(
        name: Name,
        processor: (FirClassifierSymbol<*>, ConeSubstitutor) -> Unit
    ) {
        val matchedClass = classIndex[name] ?: return
        val substitution = matchedClass.typeParameterSymbols.associateWith { it.toConeType() }
        processor(matchedClass, substitutorByMap(substitution, useSiteSession))
    }

    override fun getClassifierNames(): Set<Name> = classIndex.keys

    @DelicateScopeAPI
    override fun withReplacedSessionOrNull(newSession: FirSession, newScopeSession: ScopeSession): FirScriptDeclarationsScope {
        return FirScriptDeclarationsScope(newSession, script)
    }
}
