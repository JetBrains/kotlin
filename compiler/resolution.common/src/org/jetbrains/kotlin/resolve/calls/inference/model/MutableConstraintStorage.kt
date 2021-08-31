/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.inference.model

import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintSystemUtilContext
import org.jetbrains.kotlin.resolve.calls.tower.isSuccess
import org.jetbrains.kotlin.types.model.*
import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.kotlin.utils.addToStdlib.trimToSize

private typealias Context = TypeSystemInferenceExtensionContext

class MutableVariableWithConstraints private constructor(
    private val context: Context,
    override val typeVariable: TypeVariableMarker,
    constraints: List<Constraint>? // assume simplified and deduplicated
) : VariableWithConstraints {

    constructor(context: Context, typeVariable: TypeVariableMarker) : this(context, typeVariable, null)

    constructor(context: Context, other: VariableWithConstraints) : this(context, other.typeVariable, other.constraints)

    override val constraints: List<Constraint>
        get() {
            if (simplifiedConstraints == null) {
                simplifiedConstraints = mutableConstraints.simplifyConstraints()
            }
            return simplifiedConstraints!!
        }

    // see @OnlyInputTypes annotation
    fun getProjectedInputCallTypes(utilContext: ConstraintSystemUtilContext): Collection<Pair<KotlinTypeMarker, ConstraintKind>> {
        return with(utilContext) {
            mutableConstraints
                .mapNotNullTo(SmartList()) {
                    if (it.position.from is OnlyInputTypeConstraintPosition || it.inputTypePositionBeforeIncorporation != null)
                        it.type.unCapture() to it.kind
                    else null
                }
        }
    }

    private val mutableConstraints = if (constraints == null) SmartList() else SmartList(constraints)

    private var simplifiedConstraints: SmartList<Constraint>? = mutableConstraints

    val rawConstraintsCount get() = mutableConstraints.size

    // return new actual constraint, if this constraint is new, otherwise return already existed not redundant constraint
    // the second element of pair is a flag whether a constraint was added in fact
    fun addConstraint(constraint: Constraint): Pair<Constraint, Boolean> {
        val isLowerAndFlexibleTypeWithDefNotNullLowerBound = constraint.isLowerAndFlexibleTypeWithDefNotNullLowerBound()

        for (previousConstraint in constraints) {
            if (previousConstraint.typeHashCode == constraint.typeHashCode
                && previousConstraint.type == constraint.type
                && previousConstraint.isNullabilityConstraint == constraint.isNullabilityConstraint
            ) {
                if (newConstraintIsUseless(previousConstraint, constraint)) {
                    return previousConstraint to false
                }

                val isMatchingForSimplification = when (previousConstraint.kind) {
                    ConstraintKind.LOWER -> constraint.kind.isUpper()
                    ConstraintKind.UPPER -> constraint.kind.isLower()
                    ConstraintKind.EQUALITY -> true
                }
                if (isMatchingForSimplification) {
                    val actualConstraint = if (constraint.kind != ConstraintKind.EQUALITY) {
                        Constraint(
                            ConstraintKind.EQUALITY,
                            constraint.type,
                            constraint.position,
                            constraint.typeHashCode,
                            derivedFrom = constraint.derivedFrom,
                            isNullabilityConstraint = false
                        )
                    } else constraint
                    mutableConstraints.add(actualConstraint)
                    simplifiedConstraints = null
                    return actualConstraint to true
                }
            }

            if (isLowerAndFlexibleTypeWithDefNotNullLowerBound &&
                previousConstraint.isStrongerThanLowerAndFlexibleTypeWithDefNotNullLowerBound(constraint)
            ) {
                return previousConstraint to false
            }
        }

        mutableConstraints.add(constraint)
        if (simplifiedConstraints != null && simplifiedConstraints !== mutableConstraints) {
            simplifiedConstraints!!.add(constraint)
        }

        if (simplifiedConstraints != null && isLowerAndFlexibleTypeWithDefNotNullLowerBound) {
            simplifiedConstraints = null
        }

        return constraint to true
    }

    // This method should be used only for transaction in constraint system
    // shouldRemove should give true only for tail elements
    internal fun removeLastConstraints(sinceIndex: Int) {
        mutableConstraints.trimToSize(sinceIndex)
        if (simplifiedConstraints !== mutableConstraints) {
            simplifiedConstraints = null
        }
    }

    // This method should be used only when constraint system has state COMPLETION
    internal fun removeConstrains(shouldRemove: (Constraint) -> Boolean) {
        mutableConstraints.removeAll(shouldRemove)
        if (simplifiedConstraints !== mutableConstraints) {
            simplifiedConstraints = null
        }
    }

    private fun newConstraintIsUseless(old: Constraint, new: Constraint): Boolean {
        // Constraints from declared upper bound are quite special -- they aren't considered as a proper ones
        // In other words, user-defined constraints have "higher" priority and here we're trying not to loose them
        if (old.position.from is DeclaredUpperBoundConstraintPosition<*> && new.position.from !is DeclaredUpperBoundConstraintPosition<*>)
            return false

        /*
         * We discriminate upper expected type constraints during finding a result type to fix variable (see ResultTypeResolver.kt):
         * namely, we don't intersect the expected type with other upper constraints' types to prevent cases like this:
         *  fun <T : String> materialize(): T = null as T
         *  val bar: Int = materialize() // T is inferred into String & Int without discriminating upper expected type constraints
         * So here we shouldn't lose upper non-expected type constraints.
         */
        if (old.position.from is ExpectedTypeConstraintPosition<*> && new.position.from !is ExpectedTypeConstraintPosition<*> && old.kind.isUpper() && new.kind.isUpper())
            return false

        return when (old.kind) {
            ConstraintKind.EQUALITY -> true
            ConstraintKind.LOWER -> new.kind.isLower()
            ConstraintKind.UPPER -> new.kind.isUpper()
        }
    }

    private fun SmartList<Constraint>.simplifyConstraints(): SmartList<Constraint> =
        simplifyLowerConstraints().simplifyEqualityConstraints()

    private fun SmartList<Constraint>.simplifyLowerConstraints(): SmartList<Constraint> {
        val usefulConstraints = SmartList<Constraint>()
        for (constraint in this) {
            if (!constraint.isLowerAndFlexibleTypeWithDefNotNullLowerBound()) {
                usefulConstraints.add(constraint)
                continue
            }

            // Now we have to check that some constraint T!!.T? <: K is useless or not
            // If there is constraint T..T? <: K, then the original one (T!!.T?) is useless
            // This is so because CST(T..T?, T!!..T?) == CST(T..T?)

            val thereIsStrongerConstraint = this.any { it.isStrongerThanLowerAndFlexibleTypeWithDefNotNullLowerBound(constraint) }

            if (!thereIsStrongerConstraint) {
                usefulConstraints.add(constraint)
            }
        }

        return usefulConstraints
    }

    // Such constraint is applicable for simplification
    private fun Constraint.isLowerAndFlexibleTypeWithDefNotNullLowerBound(): Boolean {
        return with(context) {
            kind == ConstraintKind.LOWER && type.isFlexible() && type.lowerBoundIfFlexible().isDefinitelyNotNullType()
        }
    }

    private fun Constraint.isStrongerThanLowerAndFlexibleTypeWithDefNotNullLowerBound(other: Constraint): Boolean {
        if (this === other) return false

        if (typeHashCode != other.typeHashCode || kind == ConstraintKind.UPPER) return false
        with(context) {
            if (!type.isFlexible() || !other.type.isFlexible()) return false
            val otherLowerBound = other.type.lowerBoundIfFlexible()
            if (!otherLowerBound.isDefinitelyNotNullType()) return false
            require(otherLowerBound is DefinitelyNotNullTypeMarker)
            val thisLowerBound = type.lowerBoundIfFlexible()
            val thisUpperBound = type.upperBoundIfFlexible()
            val otherUpperBound = other.type.upperBoundIfFlexible()
            return thisLowerBound == otherLowerBound.original() && thisUpperBound == otherUpperBound
        }
    }

    private fun SmartList<Constraint>.simplifyEqualityConstraints(): SmartList<Constraint> {
        val equalityConstraints = filter { it.kind == ConstraintKind.EQUALITY }.groupBy { it.typeHashCode }
        return when {
            equalityConstraints.isEmpty() -> this
            else -> filterTo(SmartList()) { isUsefulConstraint(it, equalityConstraints) }
        }
    }

    private fun isUsefulConstraint(constraint: Constraint, equalityConstraints: Map<Int, List<Constraint>>): Boolean {
        if (constraint.kind == ConstraintKind.EQUALITY) return true
        return equalityConstraints[constraint.typeHashCode]?.none { it.type == constraint.type } ?: true
    }

    override fun toString(): String {
        return "Constraints for $typeVariable"
    }
}


internal class MutableConstraintStorage : ConstraintStorage {
    override val allTypeVariables: MutableMap<TypeConstructorMarker, TypeVariableMarker> = LinkedHashMap()
    override val notFixedTypeVariables: MutableMap<TypeConstructorMarker, MutableVariableWithConstraints> = LinkedHashMap()
    override val missedConstraints: MutableList<Pair<IncorporationConstraintPosition, MutableList<Pair<TypeVariableMarker, Constraint>>>> =
        SmartList()
    override val initialConstraints: MutableList<InitialConstraint> = SmartList()
    override var maxTypeDepthFromInitialConstraints: Int = 1
    override val errors: MutableList<ConstraintSystemError> = SmartList()
    override val hasContradiction: Boolean get() = errors.any { !it.applicability.isSuccess }
    override val fixedTypeVariables: MutableMap<TypeConstructorMarker, KotlinTypeMarker> = LinkedHashMap()
    override val postponedTypeVariables: MutableList<TypeVariableMarker> = SmartList()
    override val builtFunctionalTypesForPostponedArgumentsByTopLevelTypeVariables: MutableMap<Pair<TypeConstructorMarker, List<Pair<TypeConstructorMarker, Int>>>, KotlinTypeMarker> =
        LinkedHashMap()
    override val builtFunctionalTypesForPostponedArgumentsByExpectedTypeVariables: MutableMap<TypeConstructorMarker, KotlinTypeMarker> =
        LinkedHashMap()
}
