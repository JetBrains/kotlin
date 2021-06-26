/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirTypeParameterRef
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutorByMap
import org.jetbrains.kotlin.fir.scopes.FirContainingNamesAwareScope
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.name.Name

class FirNestedClassifierScope(val klass: FirClass, val useSiteSession: FirSession) : FirScope(), FirContainingNamesAwareScope {
    private val classIndex: Map<Name, FirRegularClassSymbol> = run {
        val result = mutableMapOf<Name, FirRegularClassSymbol>()
        for (declaration in klass.declarations) {
            if (declaration is FirRegularClass) {
                result[declaration.name] = declaration.symbol
            }
        }
        result
    }

    fun isEmpty() = classIndex.isEmpty()

    override fun processClassifiersByNameWithSubstitution(
        name: Name,
        processor: (FirClassifierSymbol<*>, ConeSubstitutor) -> Unit
    ) {
        val matchedClass = classIndex[name] ?: return
        val substitution = klass.typeParameters.associate {
            it.symbol to it.toConeType()
        }
        processor(matchedClass, ConeSubstitutorByMap(substitution, useSiteSession))
    }

    override fun getClassifierNames(): Set<Name> = classIndex.keys

    override fun getCallableNames(): Set<Name> = emptySet()
}

fun FirTypeParameterRef.toConeType(): ConeKotlinType = ConeTypeParameterTypeImpl(ConeTypeParameterLookupTag(symbol), isNullable = false)
