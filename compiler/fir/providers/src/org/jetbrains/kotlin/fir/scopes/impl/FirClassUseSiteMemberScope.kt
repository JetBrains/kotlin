/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.scopes.FirContainingNamesAwareScope
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.isStatic
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

class FirClassUseSiteMemberScope(
    classId: ClassId,
    session: FirSession,
    superTypesScope: FirTypeScope,
    declaredMemberScope: FirContainingNamesAwareScope
) : AbstractFirUseSiteMemberScope(classId, session, FirStandardOverrideChecker(session), superTypesScope, declaredMemberScope) {

    override fun doProcessProperties(name: Name): Collection<FirVariableSymbol<*>> {
        val seen = mutableSetOf<FirVariableSymbol<*>>()
        val result = mutableSetOf<FirVariableSymbol<*>>()
        declaredMemberScope.processPropertiesByName(name) l@{
            if (it.isStatic) return@l
            if (it is FirPropertySymbol) {
                val directOverridden = computeDirectOverridden(it.fir)
                this@FirClassUseSiteMemberScope.directOverriddenProperties[it] = directOverridden
            }
            seen += it
            result += it
        }

        superTypesScope.processPropertiesByName(name) {
            val overriddenBy = it.getOverridden(seen)
            if (overriddenBy == null) {
                result += it
            }
        }
        return result
    }

    private fun computeDirectOverridden(property: FirProperty): MutableList<FirPropertySymbol> {
        val result = mutableListOf<FirPropertySymbol>()
        superTypesScope.processPropertiesByName(property.name) l@{ superSymbol ->
            if (superSymbol !is FirPropertySymbol) return@l
            if (overrideChecker.isOverriddenProperty(property, superSymbol.fir)) {
                result.add(superSymbol)
            }
        }
        return result
    }

    override fun toString(): String {
        return "Use site scope of $classId"
    }
}
