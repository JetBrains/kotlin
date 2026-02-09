/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.inference

import org.jetbrains.kotlin.fir.resolve.calls.ConeCollectionLiteralAtom
import org.jetbrains.kotlin.fir.resolve.calls.ResolutionContext
import org.jetbrains.kotlin.fir.resolve.chooseSingleClassFromIntersectionComponents
import org.jetbrains.kotlin.fir.resolve.declaresOperatorOf
import org.jetbrains.kotlin.fir.resolve.getClassRepresentativeForCollectionLiteralResolution
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.asCone
import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintSystemCompletionContext
import org.jetbrains.kotlin.resolve.calls.inference.components.TypeVariableDependencyInformationProvider
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintKind
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.kotlin.types.model.TypeVariableTypeConstructorMarker
import org.jetbrains.kotlin.types.model.typeConstructor
import org.jetbrains.kotlin.utils.SmartSet

sealed class CollectionLiteralBounds(val atom: ConeCollectionLiteralAtom) : Comparable<CollectionLiteralBounds> {
    protected abstract val readiness: CollectionLiteralReadiness

    /**
     * Several [bounds] with operator `of` found.
     */
    class Ambiguity(atom: ConeCollectionLiteralAtom, val bounds: Set<FirRegularClassSymbol>) : CollectionLiteralBounds(atom) {
        override val readiness: CollectionLiteralReadiness = CollectionLiteralReadiness.AMBIGUITY
    }

    /**
     * No bounds with operator `of` found.
     */
    class FallbackOnly(atom: ConeCollectionLiteralAtom) : CollectionLiteralBounds(atom) {
        override val readiness: CollectionLiteralReadiness = CollectionLiteralReadiness.FALLBACK_ONLY
    }

    /**
     * Single [bound] with operator `of` found.
     */
    class SingleBound(atom: ConeCollectionLiteralAtom, val bound: FirRegularClassSymbol) : CollectionLiteralBounds(atom) {
        override val readiness: CollectionLiteralReadiness = CollectionLiteralReadiness.SINGLE_BOUND
    }

    /**
     * Very special case compared with other ones. Here, [bound] is not guaranteed to be non-null.
     * Moreover, it is not guaranteed to be a class that defines `of`.
     * Atoms that are [NonTvExpected] get to be analyzed much earlier in the completion loop,
     * since we can never obtain more precise expected type for them.
     * TODO: Probably worth adding [NonTvExpected] vs `NonTvExpectedForFallback` on this level.
     */
    class NonTvExpected(atom: ConeCollectionLiteralAtom, val bound: FirRegularClassSymbol?) : CollectionLiteralBounds(atom) {
        override val readiness: CollectionLiteralReadiness = CollectionLiteralReadiness.NON_TV_EXPECTED
    }

    protected enum class CollectionLiteralReadiness {
        AMBIGUITY,
        FALLBACK_ONLY,
        SINGLE_BOUND,
        NON_TV_EXPECTED;
    }

    override fun compareTo(other: CollectionLiteralBounds): Int = readiness.compareTo(other.readiness)
}

class CollectionLiteralBoundsCollector(
    private val dependencyInformationProvider: TypeVariableDependencyInformationProvider,
) {
    context(c: ConstraintSystemCompletionContext, resolutionContext: ResolutionContext)
    fun collectBoundsForCollectionLiteral(atom: ConeCollectionLiteralAtom): CollectionLiteralBounds? {
        if (atom.analyzed) return null
        val clExpectedVariable = atom.expectedType?.typeConstructor()
            ?: return CollectionLiteralBounds.FallbackOnly(atom)

        if (clExpectedVariable !in c.notFixedTypeVariables) {
            require(clExpectedVariable !is TypeVariableTypeConstructorMarker) {
                "CL-expected type variable must not be fixed before its CL."
            }
            val classSymbol = atom.expectedType.getClassRepresentativeForCollectionLiteralResolution()
            return CollectionLiteralBounds.NonTvExpected(atom, classSymbol)
        }

        val bounds: MutableSet<FirRegularClassSymbol> = mutableSetOf()

        // recursive implementation
        fun processConstraintsOfShallowlyDependentVariable(
            dependentVariable: TypeConstructorMarker,
            visited: MutableSet<TypeConstructorMarker> = SmartSet.create()
        ) {
            visited.add(dependentVariable)

            val constraints = c.notFixedTypeVariables[dependentVariable]?.constraints ?: emptyList()

            val lowerSetOfConstraints: MutableSet<FirRegularClassSymbol> = SmartSet.create()
            val upperSetOfConstraints: MutableSet<FirRegularClassSymbol> = SmartSet.create()
            for (constraint in constraints) {
                // EQUALITY constraints are considered UPPER and LOWER simultaneously here
                if (constraint.kind != ConstraintKind.LOWER) {
                    constraint.type.asCone().getClassRepresentativeForCollectionLiteralResolution()?.let {
                        if (it.declaresOperatorOf()) upperSetOfConstraints += it
                    }
                }
                if (constraint.kind != ConstraintKind.UPPER) {
                    constraint.type.asCone().getClassRepresentativeForCollectionLiteralResolution()?.let {
                        if (it.declaresOperatorOf()) lowerSetOfConstraints += it
                    }
                }
            }

            bounds.addAll(lowerSetOfConstraints)

            when (val singleUpperBound = upperSetOfConstraints.chooseSingleClassFromIntersectionComponents()) {
                null -> bounds += upperSetOfConstraints
                else -> bounds += singleUpperBound
            }

            val otherDependentVariables = dependencyInformationProvider.getShallowlyDependentVariables(dependentVariable) ?: emptySet()

            for (otherDependentVariable in otherDependentVariables) {
                if (otherDependentVariable in visited) continue
                processConstraintsOfShallowlyDependentVariable(otherDependentVariable, visited)
            }
        }

        processConstraintsOfShallowlyDependentVariable(clExpectedVariable)

        return when (bounds.size) {
            0 -> CollectionLiteralBounds.FallbackOnly(atom)
            1 -> CollectionLiteralBounds.SingleBound(atom, bounds.single())
            else -> CollectionLiteralBounds.Ambiguity(atom, bounds)
        }
    }
}