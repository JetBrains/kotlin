/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.Name

class FirClassUseSiteMemberScope(
    klass: FirClass,
    session: FirSession,
    superTypeScopes: List<FirTypeScope>,
    declaredMemberScope: FirContainingNamesAwareScope
) : AbstractFirUseSiteMemberScope(
    klass.symbol.toLookupTag(),
    session,
    session.firOverrideChecker,
    // The checker here is used for matching supertype intersections
    // If we came here from platform (e.g. Native), we use a platform override checker
    // JavaClassUseSiteMemberScope also uses its own JavaOverrideChecker here
    // Otherwise we should use a special intersection checker (similar one is used in FirTypeIntersectionScope)
    session.firOverrideChecker.takeIf { it !is FirStandardOverrideChecker } ?: FirIntersectionScopeOverrideChecker(session),
    superTypeScopes,
    klass.defaultType(),
    declaredMemberScope
) {
    override fun collectProperties(name: Name): Collection<FirVariableSymbol<*>> {
        return buildList {
            val explicitlyDeclaredProperties = mutableSetOf<FirVariableSymbol<*>>()
            declaredMemberScope.processPropertiesByName(name) { symbol ->
                if (symbol.isStatic) return@processPropertiesByName
                if (symbol is FirPropertySymbol) {
                    val directOverridden = computeDirectOverriddenForDeclaredProperty(symbol)
                    directOverriddenProperties[symbol] = directOverridden
                }
                explicitlyDeclaredProperties += symbol
                add(symbol)
            }


            val (properties, fields) = getPropertiesAndFieldsFromSupertypesByName(name)
            for (resultOfIntersection in properties) {
                resultOfIntersection.collectNonOverriddenDeclarations(explicitlyDeclaredProperties, this@buildList)
            }
            addAll(fields)
        }
    }

    private fun computeDirectOverriddenForDeclaredProperty(declaredPropertySymbol: FirPropertySymbol): List<FirTypeIntersectionScopeContext.ResultOfIntersection<FirPropertySymbol>> {
        val result = mutableListOf<FirTypeIntersectionScopeContext.ResultOfIntersection<FirPropertySymbol>>()
        for (resultOfIntersection in getPropertiesAndFieldsFromSupertypesByName(declaredPropertySymbol.name).first) {
            resultOfIntersection.collectDirectOverriddenForDeclared(declaredPropertySymbol, result, overrideChecker::isOverriddenProperty)
        }
        return result
    }

    private fun getPropertiesAndFieldsFromSupertypesByName(name: Name): Pair<List<FirTypeIntersectionScopeContext.ResultOfIntersection<FirPropertySymbol>>, List<FirFieldSymbol>> {
        propertiesFromSupertypes[name]?.let {
            return it to fieldsFromSupertypes.getValue(name)
        }

        val fields = mutableListOf<FirFieldSymbol>()
        val properties = supertypeScopeContext.collectIntersectionResultsForCallables<FirPropertySymbol>(name) { propertyName, processor ->
            processPropertiesByName(propertyName) {
                when (it) {
                    is FirPropertySymbol -> processor(it)
                    is FirFieldSymbol -> fields += it
                    else -> {}
                }
            }
        }
        propertiesFromSupertypes[name] = properties
        fieldsFromSupertypes[name] = fields
        return properties to fields
    }

    override fun FirNamedFunctionSymbol.isVisibleInCurrentClass(): Boolean {
        return true
    }

    override fun toString(): String {
        return "Use site scope of ${ownerClassLookupTag.classId}"
    }
}
