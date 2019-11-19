/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.declarations.FirCallableMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.ProcessorAction.NEXT
import org.jetbrains.kotlin.fir.scopes.ProcessorAction.STOP
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.name.Name

class FirClassDeclaredMemberScope(klass: FirClass<*>, useLazyNestedClassifierScope: Boolean = false) : FirScope() {
    private val nestedClassifierScope = if (useLazyNestedClassifierScope) {
        nestedClassifierScope(klass.symbol.classId, klass.session)
    } else {
        nestedClassifierScope(klass)
    }

    private val callablesIndex: Map<Name, List<FirCallableSymbol<*>>> = run {
        val result = mutableMapOf<Name, MutableList<FirCallableSymbol<*>>>()
        loop@ for (declaration in klass.declarations) {
            when (declaration) {
                is FirCallableMemberDeclaration<*> -> {
                    val name = when (declaration) {
                        is FirConstructor -> if (klass is FirRegularClass) klass.name else continue@loop
                        else -> declaration.name
                    }
                    result.getOrPut(name) { mutableListOf() } += declaration.symbol
                }
            }
        }
        result
    }

    override fun processFunctionsByName(name: Name, processor: (FirFunctionSymbol<*>) -> ProcessorAction): ProcessorAction {
        val symbols = callablesIndex[name] ?: emptyList()
        for (symbol in symbols) {
            if (symbol is FirFunctionSymbol<*> && !processor(symbol)) {
                return STOP
            }
        }
        return NEXT
    }

    override fun processPropertiesByName(name: Name, processor: (FirCallableSymbol<*>) -> ProcessorAction): ProcessorAction {
        val symbols = callablesIndex[name] ?: emptyList()
        for (symbol in symbols) {
            if (symbol is FirVariableSymbol && !processor(symbol)) {
                return STOP
            }
        }
        return NEXT
    }

    override fun processClassifiersByName(
        name: Name,
        processor: (FirClassifierSymbol<*>) -> ProcessorAction
    ): ProcessorAction = nestedClassifierScope.processClassifiersByName(name, processor)
}
