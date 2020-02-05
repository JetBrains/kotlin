/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.NAME_FOR_BACKING_FIELD
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.Name

class FirLocalScope : FirScope() {

    val properties = mutableMapOf<Name, FirVariableSymbol<*>>()
    val functions = mutableMapOf<Name, MutableList<FirFunctionSymbol<*>>>()
    val classes = mutableMapOf<Name, FirRegularClassSymbol>()

    fun storeClass(klass: FirRegularClass) {
        classes[klass.name] = klass.symbol
    }

    fun storeFunction(function: FirSimpleFunction) {
        functions.getOrPut(function.name) {
            mutableListOf()
        }.add(function.symbol as FirNamedFunctionSymbol)
    }

    fun storeVariable(variable: FirVariable<*>) {
        properties[variable.name] = variable.symbol
    }

    fun storeBackingField(property: FirProperty) {
        properties[NAME_FOR_BACKING_FIELD] = property.backingFieldSymbol
    }

    override fun processFunctionsByName(name: Name, processor: (FirFunctionSymbol<*>) -> Unit) {
        for (function in functions[name].orEmpty()) {
            processor(function)
        }
    }

    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        val property = properties[name]
        if (property != null) {
            processor(property)
        }
    }

    override fun processClassifiersByName(name: Name, processor: (FirClassifierSymbol<*>) -> Unit) {
        val klass = classes[name]
        if (klass != null) {
            processor(klass)
        }
    }
}