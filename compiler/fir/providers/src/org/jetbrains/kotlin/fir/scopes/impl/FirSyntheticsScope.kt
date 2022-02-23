/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.name.Name

class FirSyntheticsScope(
    private val declaration: FirDeclaration,
) : FirTypeScope() {
    override fun processDirectOverriddenFunctionsWithBaseScope(
        functionSymbol: FirNamedFunctionSymbol,
        processor: (FirNamedFunctionSymbol, FirTypeScope) -> ProcessorAction
    ): ProcessorAction = ProcessorAction.NEXT

    override fun processDirectOverriddenPropertiesWithBaseScope(
        propertySymbol: FirPropertySymbol,
        processor: (FirPropertySymbol, FirTypeScope) -> ProcessorAction
    ): ProcessorAction = ProcessorAction.NEXT

    override fun getCallableNames(): Set<Name> = emptySet()

    override fun getClassifierNames(): Set<Name> = emptySet()

    override fun processFunctionsByName(
        name: Name,
        processor: (FirNamedFunctionSymbol) -> Unit
    ) {
    }

    override fun processPropertiesByName(
        name: Name,
        processor: (FirVariableSymbol<*>) -> Unit
    ) {
        getPropertyByName(name)?.let {
            processor(it)
        }
    }

    private val fieldName = Name.identifier("field")

    private fun getPropertyByName(name: Name) = when (name) {
        fieldName -> tryGetFieldProperty()?.symbol
        else -> null
    }

    private fun tryGetFieldProperty(): FirVariable? {
        val property = declaration as? FirProperty ?: return null
        return property.backingField
    }
}