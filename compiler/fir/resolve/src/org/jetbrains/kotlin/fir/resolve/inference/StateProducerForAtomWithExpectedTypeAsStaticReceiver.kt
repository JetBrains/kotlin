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
     * For context-sensitive resolution, the class may also block the resolution of [atom] against any other class,
     * see [ReceiverSuitability.BLOCKING].
     */
    context(resolutionContext: ResolutionContext)
    fun getReceiverSuitability(atom: A, classSymbol: FirRegularClassSymbol): ReceiverSuitability
}

enum class ReceiverSuitability {
    SUITABLE,
    NOT_SUITABLE,

    /**
     * The class is not a suitable receiver, and the atom must not be resolved against any other bound of its expected type either,
     * so its state is [StateForAtomWithExpectedTypeAsStaticReceiver.FallbackOnly] regardless of the other bounds.
     */
    BLOCKING,
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
     * No suitable bounds found, or some bound blocks the resolution, see [ReceiverSuitability.BLOCKING].
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
     * Moreover, it is not guaranteed to be a suitable receiver (see [ExpectedTypeAsStaticReceiverStrategy.getReceiverSuitability]).
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

        /**
         * recursive implementation
         * @return false when some bound blocks the resolution, see [ReceiverSuitability.BLOCKING]
         */
        fun processConstraintsOfShallowlyDependentVariable(
            dependentVariable: TypeConstructorMarker,
            visited: MutableSet<TypeConstructorMarker> = SmartSet.create()
        ): Boolean {
            visited.add(dependentVariable)

            val constraints = c.notFixedTypeVariables[dependentVariable]?.constraints ?: emptyList()

            val lowerSetOfConstraints: MutableSet<FirRegularClassSymbol> = SmartSet.create()
            val upperSetOfConstraints: MutableSet<FirRegularClassSymbol> = SmartSet.create()
            for (constraint in constraints) {
                val classSymbol = strategy.getClassRepresentative(constraint.type.asCone()) ?: continue
                when (strategy.getReceiverSuitability(atom, classSymbol)) {
                    SUITABLE -> {
                        // EQUALITY constraints are considered UPPER and LOWER simultaneously here
                        if (constraint.kind != LOWER) upperSetOfConstraints += classSymbol
                        if (constraint.kind != UPPER) lowerSetOfConstraints += classSymbol
                    }
                    NOT_SUITABLE -> {}
                    BLOCKING -> return false
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
                if (!processConstraintsOfShallowlyDependentVariable(otherDependentVariable, visited)) return false
            }

            return true
        }

        if (!processConstraintsOfShallowlyDependentVariable(expectedVariable)) {
            return StateForAtomWithExpectedTypeAsStaticReceiver.FallbackOnly(atom)
        }

        return when (bounds.size) {
            0 -> StateForAtomWithExpectedTypeAsStaticReceiver.FallbackOnly(atom)
            1 -> StateForAtomWithExpectedTypeAsStaticReceiver.SingleBound(atom, bounds.single())
            else -> StateForAtomWithExpectedTypeAsStaticReceiver.MultipleBounds(atom, bounds)
        }
    }
}
