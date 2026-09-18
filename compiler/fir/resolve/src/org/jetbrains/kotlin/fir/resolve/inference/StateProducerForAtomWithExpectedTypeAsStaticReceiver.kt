/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.inference

import org.jetbrains.kotlin.fir.resolve.calls.ConeAtomWithExpectedTypeAsStaticReceiver
import org.jetbrains.kotlin.fir.resolve.calls.ResolutionContext
import org.jetbrains.kotlin.fir.resolve.chooseMostSpecificClass
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.asCone
import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintSystemCompletionContext
import org.jetbrains.kotlin.resolve.calls.inference.components.TypeVariableDependencyInformationProvider
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintKind
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.kotlin.types.model.TypeVariableTypeConstructorMarker
import org.jetbrains.kotlin.types.model.typeConstructor
import org.jetbrains.kotlin.utils.SmartSet

interface ExpectedTypeAsStaticReceiverStrategy<in A : ConeAtomWithExpectedTypeAsStaticReceiver> {
    context(resolutionContext: ResolutionContext)
    fun getClassRepresentative(type: ConeKotlinType): FirRegularClassSymbol?

    /**
     * Whether [classSymbol] is a meaningful receiver for [atom].
     * For collection literals, that means the class declares an applicable `operator fun of`.
     */
    context(resolutionContext: ResolutionContext)
    fun isSuitableReceiver(atom: A, classSymbol: FirRegularClassSymbol): Boolean
}

sealed class StateForAtomWithExpectedTypeAsStaticReceiver<out A : ConeAtomWithExpectedTypeAsStaticReceiver>(
    val atom: A,
) : Comparable<StateForAtomWithExpectedTypeAsStaticReceiver<*>> {
    protected abstract val readiness: Readiness

    class MultipleBounds<out A : ConeAtomWithExpectedTypeAsStaticReceiver>(
        atom: A,
        val bounds: Set<FirRegularClassSymbol>,
    ) : StateForAtomWithExpectedTypeAsStaticReceiver<A>(atom) {
        override val readiness: Readiness = Readiness.MULTIPLE_BOUNDS
    }

    /**
     * No suitable bounds found.
     */
    class FallbackOnly<out A : ConeAtomWithExpectedTypeAsStaticReceiver>(atom: A) : StateForAtomWithExpectedTypeAsStaticReceiver<A>(atom) {
        override val readiness: Readiness = Readiness.FALLBACK_ONLY
    }

    class SingleBound<out A : ConeAtomWithExpectedTypeAsStaticReceiver>(
        atom: A,
        val bound: FirRegularClassSymbol,
    ) : StateForAtomWithExpectedTypeAsStaticReceiver<A>(atom) {
        override val readiness: Readiness = Readiness.SINGLE_BOUND
    }

    /**
     * Very special case compared with other ones. Here, [bound] is not guaranteed to be non-null.
     * Moreover, it is not guaranteed to be a suitable receiver (see [ExpectedTypeAsStaticReceiverStrategy.isSuitableReceiver]).
     * Atoms that are [NonTvExpected] get to be analyzed much earlier in the completion loop,
     * since we can never obtain more precise expected type for them.
     * TODO: Probably worth adding [NonTvExpected] vs `NonTvExpectedForFallback` on this level.
     */
    class NonTvExpected<out A : ConeAtomWithExpectedTypeAsStaticReceiver>(
        atom: A,
        val bound: FirRegularClassSymbol?,
    ) : StateForAtomWithExpectedTypeAsStaticReceiver<A>(atom) {
        override val readiness: Readiness = Readiness.NON_TV_EXPECTED
    }

    protected enum class Readiness {
        MULTIPLE_BOUNDS,
        FALLBACK_ONLY,
        SINGLE_BOUND,
        NON_TV_EXPECTED;
    }

    override fun compareTo(other: StateForAtomWithExpectedTypeAsStaticReceiver<*>): Int = readiness.compareTo(other.readiness)
}

class StateProducerForAtomWithExpectedTypeAsStaticReceiver<A : ConeAtomWithExpectedTypeAsStaticReceiver>(
    private val dependencyInformationProvider: TypeVariableDependencyInformationProvider,
    private val strategy: ExpectedTypeAsStaticReceiverStrategy<A>,
) {
    context(c: ConstraintSystemCompletionContext, resolutionContext: ResolutionContext)
    fun computeState(atom: A): StateForAtomWithExpectedTypeAsStaticReceiver<A>? {
        if (atom.analyzed) return null
        val expectedType = atom.expectedType ?: return StateForAtomWithExpectedTypeAsStaticReceiver.FallbackOnly(atom)
        val expectedVariable = expectedType.typeConstructor()

        if (expectedVariable !in c.notFixedTypeVariables) {
            require(expectedVariable !is TypeVariableTypeConstructorMarker) {
                "Expected type variable must not be fixed before its atom."
            }
            val classSymbol = strategy.getClassRepresentative(expectedType)
            return StateForAtomWithExpectedTypeAsStaticReceiver.NonTvExpected(atom, classSymbol)
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
                    strategy.getClassRepresentative(constraint.type.asCone())?.let {
                        if (strategy.isSuitableReceiver(atom, it)) upperSetOfConstraints += it
                    }
                }
                if (constraint.kind != ConstraintKind.UPPER) {
                    strategy.getClassRepresentative(constraint.type.asCone())?.let {
                        if (strategy.isSuitableReceiver(atom, it)) lowerSetOfConstraints += it
                    }
                }
            }

            bounds.addAll(lowerSetOfConstraints)

            when (val singleUpperBound = upperSetOfConstraints.chooseMostSpecificClass(resolutionContext.session)) {
                null -> bounds += upperSetOfConstraints
                else -> bounds += singleUpperBound
            }

            val otherDependentVariables = dependencyInformationProvider.getShallowlyDependentVariables(dependentVariable) ?: emptySet()

            for (otherDependentVariable in otherDependentVariables) {
                if (otherDependentVariable in visited) continue
                processConstraintsOfShallowlyDependentVariable(otherDependentVariable, visited)
            }
        }

        processConstraintsOfShallowlyDependentVariable(expectedVariable)

        return when (bounds.size) {
            0 -> StateForAtomWithExpectedTypeAsStaticReceiver.FallbackOnly(atom)
            1 -> StateForAtomWithExpectedTypeAsStaticReceiver.SingleBound(atom, bounds.single())
            else -> StateForAtomWithExpectedTypeAsStaticReceiver.MultipleBounds(atom, bounds)
        }
    }
}
