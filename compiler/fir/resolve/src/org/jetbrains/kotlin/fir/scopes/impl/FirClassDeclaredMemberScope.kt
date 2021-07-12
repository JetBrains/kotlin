/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isSynthetic
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.transformers.ensureResolvedForCalls
import org.jetbrains.kotlin.fir.scopes.FirContainingNamesAwareScope
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.getContainingClassifierNamesIfPresent
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.Name

class FirClassDeclaredMemberScope(
    val useSiteSession: FirSession,
    klass: FirClass,
    useLazyNestedClassifierScope: Boolean = false,
    existingNames: List<Name>? = null,
    symbolProvider: FirSymbolProvider? = null
) : FirScope(), FirContainingNamesAwareScope {
    private val nestedClassifierScope: FirScope? = if (useLazyNestedClassifierScope) {
        lazyNestedClassifierScope(klass.symbol.classId, existingNames!!, symbolProvider!!)
    } else {
        useSiteSession.nestedClassifierScope(klass)
    }

    private val callablesIndex: Map<Name, List<FirCallableSymbol<*>>> = run {
        val result = mutableMapOf<Name, MutableList<FirCallableSymbol<*>>>()
        loop@ for (declaration in klass.declarations) {
            when (declaration) {
                is FirCallableDeclaration -> {
                    val name = when (declaration) {
                        is FirConstructor -> CONSTRUCTOR_NAME
                        is FirVariable -> if (declaration.isSynthetic) continue@loop else declaration.name
                        is FirSimpleFunction -> declaration.name
                        else -> continue@loop
                    }
                    result.getOrPut(name) { mutableListOf() } += declaration.symbol
                }
            }
        }
        result
    }

    override fun processFunctionsByName(name: Name, processor: (FirNamedFunctionSymbol) -> Unit) {
        if (name == CONSTRUCTOR_NAME) return
        processCallables(name, processor)
    }

    override fun processDeclaredConstructors(processor: (FirConstructorSymbol) -> Unit) {
        processCallables(CONSTRUCTOR_NAME, processor)
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
                symbol.ensureResolvedForCalls(useSiteSession)
                processor(symbol)
            }
        }
    }

    override fun processClassifiersByNameWithSubstitution(
        name: Name,
        processor: (FirClassifierSymbol<*>, ConeSubstitutor) -> Unit
    ) {
        nestedClassifierScope?.processClassifiersByNameWithSubstitution(name, processor)
    }

    override fun getCallableNames(): Set<Name> {
        return callablesIndex.keys
    }

    override fun getClassifierNames(): Set<Name> {
        return nestedClassifierScope?.getContainingClassifierNamesIfPresent().orEmpty()
    }
}


private val CONSTRUCTOR_NAME = Name.special("<init>")
