/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.scopes

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.java.declarations.FirJavaClass
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.scopes.impl.nestedClassifierScope
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeLookupTagBasedType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

internal class JavaClassDeclaredMembersEnhancementScope(
    private val useSiteSession: FirSession,
    private val owner: FirJavaClass,
    private val useSiteMemberEnhancementScope: FirTypeScope
) : FirContainingNamesAwareScope() {
    private fun FirCallableDeclaration.isDeclared(): Boolean {
        return (this.dispatchReceiverType as? ConeLookupTagBasedType)?.lookupTag == owner.symbol.toLookupTag()
                && this.origin !is FirDeclarationOrigin.SubstitutionOverride
                && this.origin != FirDeclarationOrigin.IntersectionOverride
    }

    private val callableNames = run {
        (useSiteMemberEnhancementScope.collectAllProperties() + useSiteMemberEnhancementScope.collectAllFunctions()).filter {
            it.fir.isDeclared()
        }.map {
            it.name
        }.toSet()
    }

    private val nestedClassifierScope: FirContainingNamesAwareScope? =
        useSiteSession.nestedClassifierScope(owner)

    override fun getCallableNames(): Set<Name> {
        return callableNames
    }

    override fun getClassifierNames(): Set<Name> {
        return nestedClassifierScope?.getClassifierNames().orEmpty()
    }

    override fun processFunctionsByName(name: Name, processor: (FirNamedFunctionSymbol) -> Unit) {
        if (name == SpecialNames.INIT) return
        useSiteMemberEnhancementScope.processFunctionsByName(name) { symbol ->
            if (symbol.fir.isDeclared()) {
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

    override fun processDeclaredConstructors(processor: (FirConstructorSymbol) -> Unit) {
        useSiteMemberEnhancementScope.processDeclaredConstructors { symbol ->
            processor(symbol)
        }
    }

    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        useSiteMemberEnhancementScope.processPropertiesByName(name) { symbol ->
            if (symbol.fir.isDeclared()) {
                processor(symbol)
            }
        }
    }

    override fun toString(): String {
        return "Java enhancement declared member scope for ${owner.classId}"
    }

    private fun FirCallableDeclaration.overriddenMembers(): List<FirCallableDeclaration> {
        return when (val symbol = this.symbol) {
            is FirNamedFunctionSymbol -> useSiteMemberEnhancementScope.getDirectOverriddenMembers(symbol)
            is FirPropertySymbol -> useSiteMemberEnhancementScope.getDirectOverriddenProperties(symbol)
            else -> emptyList()
        }.map { it.fir }
    }
}