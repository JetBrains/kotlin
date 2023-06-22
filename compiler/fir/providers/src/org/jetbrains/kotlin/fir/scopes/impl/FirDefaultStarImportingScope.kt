/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.Name

/**
 * Works as composite scope that only looks to the second scope if the first one is empty.
 * It's necessary, e.g. to make sure we don't look for java.lang.String classifier/constructors once we've found kotlin.String one.
 * @see org.jetbrains.kotlin.resolve.lazy.LazyImportScope that produces similar semantics
 */
class FirDefaultStarImportingScope(
    val first: FirSingleLevelDefaultStarImportingScope,
    val second: FirSingleLevelDefaultStarImportingScope,
) : FirScope(), DefaultStarImportingScopeMarker {
    override fun processClassifiersByNameWithSubstitution(name: Name, processor: (FirClassifierSymbol<*>, ConeSubstitutor) -> Unit) {
        var wasFoundAny = false
        first.processClassifiersByNameWithSubstitution(name) { symbol, substitutor ->
            wasFoundAny = true
            processor(symbol, substitutor)
        }

        if (!wasFoundAny) {
            second.processClassifiersByNameWithSubstitution(name, processor)
        }
    }

    private fun <S : FirCallableSymbol<*>> processSymbolsByName(
        name: Name,
        processingFactory: FirScope.(Name, (S) -> Unit) -> Unit,
        processor: (S) -> Unit,
    ) {
        var wasFoundAny = false
        first.processingFactory(name) {
            wasFoundAny = true
            processor(it)
        }

        if (!wasFoundAny) {
            second.processingFactory(name, processor)
        }
    }

    override fun processFunctionsByName(name: Name, processor: (FirNamedFunctionSymbol) -> Unit) {
        processSymbolsByName(name, FirScope::processFunctionsByName, processor)
    }

    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        processSymbolsByName(name, FirScope::processPropertiesByName, processor)
    }

    override fun processDeclaredConstructors(processor: (FirConstructorSymbol) -> Unit) {
        var wasFoundAny = false
        first.processDeclaredConstructors { symbol ->
            wasFoundAny = true
            processor(symbol)
        }

        if (!wasFoundAny) {
            second.processDeclaredConstructors(processor)
        }
    }
}