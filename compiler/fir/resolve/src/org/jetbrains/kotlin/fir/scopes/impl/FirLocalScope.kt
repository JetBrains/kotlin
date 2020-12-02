/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import org.jetbrains.kotlin.fir.NAME_FOR_BACKING_FIELD
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.resolve.PersistentMultimap
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.scopes.FirContainingNamesAwareScope
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.Name

class FirLocalScope private constructor(
    val properties: PersistentMap<Name, FirVariableSymbol<*>>,
    val functions: PersistentMultimap<Name, FirFunctionSymbol<*>>,
    val classes: PersistentMap<Name, FirRegularClassSymbol>
) : FirScope(), FirContainingNamesAwareScope {
    constructor() : this(persistentMapOf(), PersistentMultimap(), persistentMapOf())

    fun storeClass(klass: FirRegularClass): FirLocalScope {
        return FirLocalScope(
            properties, functions, classes.put(klass.name, klass.symbol)
        )
    }

    fun storeFunction(function: FirSimpleFunction): FirLocalScope {
        return FirLocalScope(
            properties, functions.put(function.name, function.symbol), classes
        )
    }

    fun storeVariable(variable: FirVariable<*>): FirLocalScope {
        return FirLocalScope(
            properties.put(variable.name, variable.symbol), functions, classes
        )
    }

    fun storeBackingField(property: FirProperty): FirLocalScope {
        return FirLocalScope(
            properties.put(NAME_FOR_BACKING_FIELD, property.backingFieldSymbol), functions, classes
        )
    }

    override fun processFunctionsByName(name: Name, processor: (FirFunctionSymbol<*>) -> Unit) {
        for (function in functions[name]) {
            processor(function)
        }
    }

    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        val property = properties[name]
        if (property != null) {
            processor(property)
        }
    }

    override fun processClassifiersByNameWithSubstitution(name: Name, processor: (FirClassifierSymbol<*>, ConeSubstitutor) -> Unit) {
        val klass = classes[name]
        if (klass != null) {
            processor(klass, ConeSubstitutor.Empty)
        }
    }

    override fun mayContainName(name: Name) = properties.containsKey(name) || functions[name].isNotEmpty() || classes.containsKey(name)

    override fun getCallableNames(): Set<Name> = properties.keys + functions.keys
    override fun getClassifierNames(): Set<Name> = classes.keys
}
