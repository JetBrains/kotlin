/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.scopes

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.containingClassForStaticMemberAttr
import org.jetbrains.kotlin.fir.java.JavaTypeParameterStack
import org.jetbrains.kotlin.fir.originalOrSelf
import org.jetbrains.kotlin.fir.resolve.ScopeSessionKey
import org.jetbrains.kotlin.fir.scopes.FirContainingNamesAwareScope
import org.jetbrains.kotlin.fir.scopes.impl.FirFakeOverrideGenerator
import org.jetbrains.kotlin.fir.scopes.impl.FirSubstitutionOverrideScope
import org.jetbrains.kotlin.fir.scopes.impl.substitutionOverrideStorage
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

data class JavaClassStaticUseSiteScopeKey(val classId: ClassId) :
    ScopeSessionKey<Nothing, Nothing>()

class JavaClassStaticUseSiteScope internal constructor(
    private val session: FirSession,
    private val owner: ConeClassLikeLookupTag,
    private val declaredMemberScope: FirContainingNamesAwareScope,
    private val superClassScope: FirContainingNamesAwareScope,
    private val superTypesScopes: List<FirContainingNamesAwareScope>,
    javaTypeParameterStack: JavaTypeParameterStack,
) : FirContainingNamesAwareScope(), FirSubstitutionOverrideScope {
    private val functions = hashMapOf<Name, Collection<FirNamedFunctionSymbol>>()
    private val properties = hashMapOf<Name, Collection<FirVariableSymbol<*>>>()
    private val overrideChecker = JavaOverrideChecker(session, javaTypeParameterStack, baseScopes = null, considerReturnTypeKinds = false)

    private val substitutionCache =
        session.substitutionOverrideStorage.substitutionOverrideCacheByScope
            .getValue(JavaClassStaticUseSiteScopeKey(owner.classId), null)

    override fun processFunctionsByName(name: Name, processor: (FirNamedFunctionSymbol) -> Unit) {
        functions.getOrPut(name) {
            computeFunctions(name)
        }.forEach(processor)
    }

    private fun computeFunctions(name: Name): MutableList<FirNamedFunctionSymbol> {
        val declaredMembers = mutableListOf<FirNamedFunctionSymbol>()
        declaredMemberScope.processFunctionsByName(name) l@{
            if (!it.isStatic) return@l
            declaredMembers.add(it)
        }

        val all = declaredMembers.toMutableList()
        superClassScope.processFunctionsByName(name) l@{
            if (!it.isStatic || declaredMembers.any { override -> overrideChecker.isOverriddenFunction(override.fir, it.fir) }) return@l
            all.add(substitutionCache.overridesForFunctions.getValue(it, this))
        }
        return all
    }

    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        return properties.getOrPut(name) {
            computeProperties(name)
        }.forEach(processor)

    }

    private fun computeProperties(name: Name): Collection<FirVariableSymbol<*>> {
        val result: MutableList<FirVariableSymbol<*>> = mutableListOf()
        declaredMemberScope.processPropertiesByName(name) l@{ propertySymbol ->
            if (!propertySymbol.isStatic) return@l
            result.add(propertySymbol)
        }

        if (result.isNotEmpty()) return result

        val seen: MutableSet<FirVariableSymbol<*>> = mutableSetOf()
        for (superTypesScope in superTypesScopes) {
            superTypesScope.processPropertiesByName(name) l@{ propertySymbol ->
                if (!propertySymbol.isStatic || !seen.add(propertySymbol.originalOrSelf())) return@l

                if (propertySymbol is FirPropertySymbol || propertySymbol is FirFieldSymbol) {
                    result.add(substitutionCache.overridesForVariables.getValue(propertySymbol, this))
                } else {
                    result.add(propertySymbol)
                }
            }
        }

        return result
    }

    override fun createSubstitutionOverride(original: FirConstructorSymbol): FirConstructorSymbol =
        throw AssertionError("constructors cannot be static")

    override fun createSubstitutionOverride(original: FirNamedFunctionSymbol): FirNamedFunctionSymbol {
        val symbol = FirFakeOverrideGenerator.createSymbolForSubstitutionOverride(original, owner.classId)
        return FirFakeOverrideGenerator.createSubstitutionOverrideFunction(session, symbol, original.fir, null).also {
            it.fir.containingClassForStaticMemberAttr = owner
        }
    }

    override fun createSubstitutionOverride(original: FirPropertySymbol): FirPropertySymbol {
        val symbol = FirFakeOverrideGenerator.createSymbolForSubstitutionOverride(original, owner.classId)
        return FirFakeOverrideGenerator.createSubstitutionOverrideProperty(session, symbol, original.fir, null).also {
            it.fir.containingClassForStaticMemberAttr = owner
        }
    }

    override fun createSubstitutionOverride(original: FirFieldSymbol): FirFieldSymbol =
        FirFakeOverrideGenerator.createSubstitutionOverrideField(
            // In Java classes, field initializers refer to the ConstantValue JVM attribute, so they're safe to copy.
            session, original.fir, original, null, owner.classId, withInitializer = true
        ).also { it.fir.containingClassForStaticMemberAttr = owner }

    override fun getCallableNames(): Set<Name> {
        return buildSet {
            addAll(declaredMemberScope.getCallableNames())
            for (superTypesScope in superTypesScopes) {
                addAll(superTypesScope.getCallableNames())
            }
        }
    }

    override fun getClassifierNames(): Set<Name> {
        return buildSet {
            addAll(declaredMemberScope.getClassifierNames())
            for (superTypesScope in superTypesScopes) {
                addAll(superTypesScope.getClassifierNames())
            }
        }
    }

    override fun mayContainName(name: Name): Boolean {
        return declaredMemberScope.mayContainName(name) || superTypesScopes.any { it.mayContainName(name) }
    }

    override val scopeOwnerLookupNames: List<String>
        get() = declaredMemberScope.scopeOwnerLookupNames
}
