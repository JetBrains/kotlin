/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.types.AbstractTypeChecker

class TypeUnifier(private val session: FirSession, private val typeParameterSymbols: List<FirTypeParameterSymbol>) {
    /**
     * Finds a substitution S that turns {@code projectWithVariables} to {@code knownProjection}.
     *
     * Example:
     *      known = List<String>
     *      withVariables = List<X>
     *      variables = {X}
     *
     *      result = X -> String
     *
     * Only types accepted by {@code isVariable} are considered variables.
     */
    fun unify(knownProjection: ConeKotlinTypeProjection, projectWithVariables: ConeKotlinTypeProjection): UnificationResult {
        val result = UnificationResult()
        doUnify(knownProjection, projectWithVariables, result)
        return result
    }

    fun doUnify(knownProjection: ConeTypeProjection, projectWithVariables: ConeTypeProjection, result: UnificationResult) {
        val firstType = knownProjection.type

        if (firstType is ConeIntersectionType) {
            val intersectionResult = mutableMapOf<FirTypeParameterSymbol, ConeKotlinType>()

            for (intersectedType in firstType.intersectedTypes) {
                val localResult = UnificationResult()
                doUnify(intersectedType, projectWithVariables, localResult)

                for ((typeParameterSymbol, typeParameterType) in localResult.substitution) {
                    val existingTypeParameterType = intersectionResult[typeParameterSymbol]
                    if (existingTypeParameterType == null ||
                        AbstractTypeChecker.isSubtypeOf(session.typeContext, typeParameterType, existingTypeParameterType)
                    ) {
                        intersectionResult[typeParameterSymbol] = typeParameterType
                    }
                }
            }

            for ((typeParameterSymbol, typeParameterType) in intersectionResult) {
                result.put(typeParameterSymbol, typeParameterType)
            }

            return
        }

        val known = firstType?.lowerBoundIfFlexible() ?: session.builtinTypes.nullableAnyType.type
        val withVariables = projectWithVariables.type?.lowerBoundIfFlexible() ?: session.builtinTypes.nullableAnyType.type

        // in Foo ~ in X  =>  Foo ~ X
        val knownProjectionKind = knownProjection.kind
        val withVariablesProjectionKind = projectWithVariables.kind
        if (knownProjectionKind == withVariablesProjectionKind && knownProjectionKind != ProjectionKind.INVARIANT) {
            doUnify(known, withVariables, result)
            return
        }

        // Foo? ~ X?  =>  Foo ~ X
        if (known.isMarkedNullable && withVariables.isMarkedNullable) {
            doUnify(
                known.withNullability(ConeNullability.NOT_NULL, session.typeContext).toTypeProjection(
                    knownProjectionKind
                ) as ConeKotlinTypeProjection,
                withVariables.withNullability(ConeNullability.NOT_NULL, session.typeContext).toTypeProjection(
                    withVariablesProjectionKind
                ) as ConeKotlinTypeProjection,
                result
            )
        }

        // in Foo ~ out X  => fail
        // in Foo ~ X  =>  may be OK
        if (knownProjectionKind != withVariablesProjectionKind && withVariablesProjectionKind != ProjectionKind.INVARIANT) {
            result.fail()
            return
        }

        // Foo ~ X? => fail
        if (!known.isMarkedNullable && withVariables.isMarkedNullable) {
            result.fail()
            return
        }

        // Foo ~ X  =>  x |-> Foo
        // * ~ X => x |-> *
        val maybeVariable = withVariables.toSymbol(session)
        if (maybeVariable is FirTypeParameterSymbol && typeParameterSymbols.contains(maybeVariable)) {
            result.put(maybeVariable, known)
            return
        }

        // Foo? ~ Foo || in Foo ~ Foo || Foo ~ Bar
        val structuralMismatch = known.isMarkedNullable != withVariables.isMarkedNullable ||
                knownProjectionKind != withVariablesProjectionKind ||
                known.toSymbol(session) != maybeVariable
        if (structuralMismatch) {
            result.fail()
            return
        }

        // Foo<A> ~ Foo<B, C>
        if (known.typeArguments.size != withVariables.typeArguments.size) {
            result.fail()
            return
        }

        // Foo ~ Foo
        if (known.typeArguments.isEmpty()) {
            return
        }

        // Foo<...> ~ Foo<...>
        val knownArguments = known.typeArguments
        val withVariablesArguments = withVariables.typeArguments
        for (index in knownArguments.indices) {
            val knownArg = knownArguments[index]
            val withVariablesArg = withVariablesArguments[index]
            doUnify(knownArg, withVariablesArg, result)
        }
    }
}

class UnificationResult {
    private var success: Boolean = true
    private var failedVariables: MutableSet<FirTypeParameterSymbol> = mutableSetOf()
    private val _substitution: MutableMap<FirTypeParameterSymbol, ConeKotlinType> = mutableMapOf()
    val substitution: Map<FirTypeParameterSymbol, ConeKotlinType>
        get() = _substitution

    fun fail() {
        success = false
    }

    fun put(key: FirTypeParameterSymbol, value: ConeKotlinType) {
        if (failedVariables.contains(key)) return

        if (substitution.containsKey(key)) {
            _substitution.remove(key)
            failedVariables.add(key)
            fail()
        } else {
            _substitution[key] = value
        }
    }
}