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
import org.jetbrains.kotlin.fir.symbols.*
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.name.Name

class FirClassDeclaredMemberScope(klass: FirRegularClass) : FirScope {
    private val callablesIndex: Map<Name, List<FirCallableSymbol>> by lazy {
        val result = mutableMapOf<Name, MutableList<FirCallableSymbol>>()
        for (declaration in klass.declarations) {
            if (declaration is FirCallableMemberDeclaration) {
                val name = if (declaration is FirConstructor) klass.name else declaration.name
                val list = result.getOrPut(name) { mutableListOf() }
                list += declaration.symbol
            }
        }
        result
    }
    private val classIndex: Map<Name, FirClassSymbol> by lazy {
        val result = mutableMapOf<Name, FirClassSymbol>()
        for (declaration in klass.declarations) {
            if (declaration is FirRegularClass) {
                result[declaration.name] = declaration.symbol
            }
        }
        result
    }

    override fun processFunctionsByName(name: Name, processor: (ConeFunctionSymbol) -> ProcessorAction): ProcessorAction {
        val matchedClass = classIndex[name]

        if (matchedClass != null) {
            if (FirClassDeclaredMemberScope(matchedClass.fir).processFunctionsByName(name, processor) == STOP) {
                return STOP
            }
        }
        val symbols = callablesIndex[name] ?: emptyList()
        for (symbol in symbols) {
            if (symbol is ConeFunctionSymbol && !processor(symbol)) {
                return STOP
            }
        }
        return NEXT
    }

    override fun processPropertiesByName(name: Name, processor: (ConeVariableSymbol) -> ProcessorAction): ProcessorAction {
        val symbols = callablesIndex[name] ?: emptyList()
        for (symbol in symbols) {
            if (symbol is ConePropertySymbol && !processor(symbol)) {
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
