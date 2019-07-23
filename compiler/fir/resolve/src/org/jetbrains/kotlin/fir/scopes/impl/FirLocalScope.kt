/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirBackingFieldReference
import org.jetbrains.kotlin.fir.declarations.FirNamedDeclaration
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.expressions.FirVariable
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.name.Name

class FirLocalScope : FirScope() {

    val properties = mutableMapOf<Name, FirVariableSymbol<*>>()
    val functions = mutableMapOf<Name, FirFunctionSymbol<*>>()

    fun storeDeclaration(declaration: FirNamedDeclaration) {
        when (declaration) {
            is FirVariable<*> -> properties[declaration.name] = declaration.symbol
            is FirNamedFunction -> functions[declaration.name] = declaration.symbol as FirNamedFunctionSymbol
        }
    }

    fun storeBackingField(property: FirProperty) {
        properties[FirBackingFieldReference.NAME] = property.backingFieldSymbol
    }

    override fun processFunctionsByName(name: Name, processor: (FirFunctionSymbol<*>) -> ProcessorAction): ProcessorAction {
        val prop = functions[name]
        if (prop != null) {
            return processor(prop)
        }
        return ProcessorAction.NEXT
    }

    override fun processPropertiesByName(name: Name, processor: (FirCallableSymbol<*>) -> ProcessorAction): ProcessorAction {
        val prop = properties[name]
        if (prop != null) {
            return processor(prop)
        }
        return ProcessorAction.NEXT
    }
}