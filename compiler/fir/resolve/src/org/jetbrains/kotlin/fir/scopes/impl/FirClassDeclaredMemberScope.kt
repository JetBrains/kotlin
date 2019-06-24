/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.declarations.FirCallableMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.scopes.FirPosition
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.ProcessorAction.NEXT
import org.jetbrains.kotlin.fir.scopes.ProcessorAction.STOP
import org.jetbrains.kotlin.fir.service
import org.jetbrains.kotlin.fir.symbols.*
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.name.Name

class FirClassDeclaredMemberScopeProvider {

    val cache = mutableMapOf<FirRegularClass, FirClassDeclaredMemberScope>()
    fun declaredMemberScope(klass: FirRegularClass): FirClassDeclaredMemberScope {
        return cache.getOrPut(klass) {
            FirClassDeclaredMemberScope(klass)
        }
    }
}

fun declaredMemberScope(klass: FirRegularClass): FirClassDeclaredMemberScope {
    return klass
        .session
        .service<FirClassDeclaredMemberScopeProvider>()
        .declaredMemberScope(klass)
}

class FirClassDeclaredMemberScope(klass: FirRegularClass) : FirScope() {
    private val callablesIndex: Map<Name, List<FirCallableSymbol<*>>> = run {
        val result = mutableMapOf<Name, MutableList<FirCallableSymbol<*>>>()
        for (declaration in klass.declarations) {
            when (declaration) {
                is FirCallableMemberDeclaration<*> -> {
                    val name = if (declaration is FirConstructor) klass.name else declaration.name
                    result.getOrPut(name) { mutableListOf() } += declaration.symbol
                }
                is FirRegularClass -> {
                    for (nestedDeclaration in declaration.declarations) {
                        if (nestedDeclaration is FirConstructor) {
                            result.getOrPut(declaration.name) { mutableListOf() } += nestedDeclaration.symbol
                        }
                    }
                }
            }
        }
        result
    }
    private val classIndex: Map<Name, FirClassSymbol> = run {
        val result = mutableMapOf<Name, FirClassSymbol>()
        for (declaration in klass.declarations) {
            if (declaration is FirRegularClass) {
                result[declaration.name] = declaration.symbol
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
            if (symbol is ConeVariableSymbol && !processor(symbol)) {
                return STOP
            }
        }
        return NEXT
    }

    override fun processClassifiersByName(name: Name, position: FirPosition, processor: (ConeClassifierSymbol) -> Boolean): Boolean {
        val matchedClass = classIndex[name]
        if (matchedClass != null && !processor(matchedClass)) {
            return false
        }

        return super.processClassifiersByName(name, position, processor)
    }
}
