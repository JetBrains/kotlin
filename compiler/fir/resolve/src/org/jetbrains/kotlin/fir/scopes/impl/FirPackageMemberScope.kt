/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.firSymbolProvider
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.transformers.ensureResolvedForCalls
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class FirPackageMemberScope(val fqName: FqName, val session: FirSession) : FirScope() {
    private val symbolProvider = session.firSymbolProvider
    private val classifierCache: MutableMap<Name, FirClassifierSymbol<*>?> = mutableMapOf()
    private val callableCache: MutableMap<Name, List<FirCallableSymbol<*>>> = mutableMapOf()

    override fun processClassifiersByNameWithSubstitution(
        name: Name,
        processor: (FirClassifierSymbol<*>, ConeSubstitutor) -> Unit
    ) {
        if (name.asString().isEmpty()) return

        val symbol = classifierCache.getOrPut(name) {
            val unambiguousFqName = ClassId(fqName, name)
            symbolProvider.getClassLikeSymbolByFqName(unambiguousFqName)
        }

        if (symbol != null) {
            processor(symbol, ConeSubstitutor.Empty)
        }
    }

    override fun processFunctionsByName(name: Name, processor: (FirNamedFunctionSymbol) -> Unit) {
        processCallables(name, processor)
    }

    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        processCallables(name, processor)
    }

    private inline fun <reified D : FirCallableSymbol<*>> processCallables(
        name: Name,
        processor: (D) -> Unit
    ) {
        val symbols = callableCache.getOrPut(name) {
            symbolProvider.getTopLevelCallableSymbols(fqName, name)
        }
        for (symbol in symbols) {
            if (symbol is D) {
                symbol.ensureResolvedForCalls(session)
                processor(symbol)
            }
        }
    }
}
