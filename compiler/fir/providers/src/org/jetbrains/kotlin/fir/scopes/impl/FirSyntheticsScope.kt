/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.declarations.utils.hasBackingField
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.scope
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.scopes.FakeOverrideTypeCalculator
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.name.Name

val SELF_NAME = Name.identifier("self")

open class FirSyntheticsScope : FirTypeScope() {
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
}

class FirPropertyWithFieldSyntheticsScope(private val declaration: FirProperty) : FirSyntheticsScope() {
    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        when (name) {
            StandardNames.BACKING_FIELD -> declaration.backingField?.let { processor(it.symbol) }
            SELF_NAME -> processor(declaration.symbol)
            else -> {}
        }
    }
}

class FirDelegatedPropertySyntheticsScope(
    private val declaration: FirProperty,
    private val delegateScope: FirTypeScope,
) : FirSyntheticsScope() {
    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        when (name) {
            SELF_NAME -> processor(declaration.symbol)
            else -> delegateScope.processPropertiesByName(name, processor)
        }
    }

    override fun processFunctionsByName(name: Name, processor: (FirNamedFunctionSymbol) -> Unit) {
        delegateScope.processFunctionsByName(name, processor)
    }

    override fun processClassifiersByNameWithSubstitution(name: Name, processor: (FirClassifierSymbol<*>, ConeSubstitutor) -> Unit) {
        delegateScope.processClassifiersByNameWithSubstitution(name, processor)
    }
}

class FirSelfOnlySyntheticsScope(private val declaration: FirVariable) : FirSyntheticsScope() {
    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        when (name) {
            SELF_NAME -> processor(declaration.symbol)
            else -> {}
        }
    }
}

fun createSyntheticsScopeFor(
    declaration: FirDeclaration,
    useSiteSession: FirSession,
    scopeSession: ScopeSession,
) = when (declaration) {
    is FirProperty -> when {
        declaration.delegate != null -> {
            val delegateType = declaration.delegate?.typeRef?.coneType
                ?: error("Should've had a delegate")
            val scope = delegateType.scope(useSiteSession, scopeSession, FakeOverrideTypeCalculator.DoNothing)
                ?: error("Couldn't get a type scope")
            FirDelegatedPropertySyntheticsScope(declaration, scope)
        }
        declaration.hasBackingField -> {
            FirPropertyWithFieldSyntheticsScope(declaration)
        }
        else -> {
            FirSelfOnlySyntheticsScope(declaration)
        }
    }
    is FirVariable -> FirSelfOnlySyntheticsScope(declaration)
    else -> null
}
