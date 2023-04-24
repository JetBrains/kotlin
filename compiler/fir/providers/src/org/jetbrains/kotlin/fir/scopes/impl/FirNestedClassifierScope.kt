/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.declarations.FirTypeParameterRef
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutorByMap
import org.jetbrains.kotlin.fir.scopes.FirContainingNamesAwareScope
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled

abstract class FirNestedClassifierScope(val klass: FirClass, val useSiteSession: FirSession) : FirContainingNamesAwareScope() {
    protected abstract fun getNestedClassSymbol(name: Name): FirClassLikeSymbol<*>?

    override fun processClassifiersByNameWithSubstitution(
        name: Name,
        processor: (FirClassifierSymbol<*>, ConeSubstitutor) -> Unit
    ) {
        val matchedClass = getNestedClassSymbol(name) ?: return
        val substitutor = if (klass.typeParameters.isEmpty()) {
            ConeSubstitutor.Empty
        } else {
            val substitution = klass.typeParameters.associate {
                it.symbol to it.toConeType()
            }
            ConeSubstitutorByMap(substitution, useSiteSession)
        }
        processor(matchedClass, substitutor)
    }

    abstract fun isEmpty(): Boolean

    override fun getCallableNames(): Set<Name> = emptySet()
}

class FirNestedClassifierScopeImpl(klass: FirClass, useSiteSession: FirSession) : FirNestedClassifierScope(klass, useSiteSession) {
    private val classIndex: Map<Name, FirClassLikeSymbol<*>> = run {
        val result = mutableMapOf<Name, FirClassLikeSymbol<*>>()
        for (declaration in klass.declarations) {
            when (declaration) {
                is FirRegularClass -> result[declaration.name] = declaration.symbol
                is FirTypeAlias -> result[declaration.name] = declaration.symbol
                else -> {}
            }
        }
        result
    }

    override fun getNestedClassSymbol(name: Name): FirClassLikeSymbol<*>? {
        return classIndex[name]
    }

    override fun isEmpty(): Boolean = classIndex.isEmpty()

    override fun getClassifierNames(): Set<Name> = classIndex.keys
}

class FirCompositeNestedClassifierScope(
    val scopes: List<FirNestedClassifierScope>,
    klass: FirClass,
    useSiteSession: FirSession
) : FirNestedClassifierScope(klass, useSiteSession) {
    override fun getNestedClassSymbol(name: Name): FirRegularClassSymbol? {
        shouldNotBeCalled()
    }

    override fun processClassifiersByNameWithSubstitution(name: Name, processor: (FirClassifierSymbol<*>, ConeSubstitutor) -> Unit) {
        scopes.forEach { it.processClassifiersByNameWithSubstitution(name, processor) }
    }

    override fun isEmpty(): Boolean {
        return scopes.all { it.isEmpty() }
    }

    override fun getClassifierNames(): Set<Name> {
        return scopes.flatMapTo(mutableSetOf()) { it.getClassifierNames() }
    }
}

fun FirTypeParameterRef.toConeType(): ConeKotlinType = symbol.toConeType()

fun FirTypeParameterSymbol.toConeType(): ConeKotlinType = ConeTypeParameterTypeImpl(ConeTypeParameterLookupTag(this), isNullable = false)
