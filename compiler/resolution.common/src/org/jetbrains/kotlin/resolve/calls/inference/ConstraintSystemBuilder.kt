/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.inference

import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintKind
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintPosition
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintSystemError
import org.jetbrains.kotlin.resolve.calls.inference.model.SimpleConstraintSystemConstraintPosition
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.kotlin.types.model.TypeSubstitutorMarker
import org.jetbrains.kotlin.types.model.TypeVariableMarker
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

interface ConstraintSystemOperation {
    val hasContradiction: Boolean
    fun registerVariable(variable: TypeVariableMarker)
    fun markPostponedVariable(variable: TypeVariableMarker)
    fun markCouldBeResolvedWithUnrestrictedBuilderInference()
    fun unmarkPostponedVariable(variable: TypeVariableMarker)
    fun removePostponedVariables()
    fun substituteFixedVariables(substitutor: TypeSubstitutorMarker)

    fun getBuiltFunctionalExpectedTypeForPostponedArgument(
        topLevelVariable: TypeConstructorMarker,
        pathToExpectedType: List<Pair<TypeConstructorMarker, Int>>
    ): KotlinTypeMarker?

    fun getBuiltFunctionalExpectedTypeForPostponedArgument(expectedTypeVariable: TypeConstructorMarker): KotlinTypeMarker?

    fun putBuiltFunctionalExpectedTypeForPostponedArgument(
        topLevelVariable: TypeConstructorMarker,
        pathToExpectedType: List<Pair<TypeConstructorMarker, Int>>,
        builtFunctionalType: KotlinTypeMarker
    )

    fun putBuiltFunctionalExpectedTypeForPostponedArgument(
        expectedTypeVariable: TypeConstructorMarker,
        builtFunctionalType: KotlinTypeMarker
    )

    fun addSubtypeConstraint(lowerType: KotlinTypeMarker, upperType: KotlinTypeMarker, position: ConstraintPosition)
    fun addEqualityConstraint(a: KotlinTypeMarker, b: KotlinTypeMarker, position: ConstraintPosition)

    fun isProperType(type: KotlinTypeMarker): Boolean
    fun isTypeVariable(type: KotlinTypeMarker): Boolean
    fun isPostponedTypeVariable(typeVariable: TypeVariableMarker): Boolean

    fun getProperSuperTypeConstructors(type: KotlinTypeMarker): List<TypeConstructorMarker>

    fun addOtherSystem(otherSystem: ConstraintStorage)

    val errors: List<ConstraintSystemError>
}

abstract class ConstraintSystemTransaction {
    abstract fun closeTransaction()

    abstract fun rollbackTransaction()
}

interface ConstraintSystemBuilder : ConstraintSystemOperation {
    fun prepareTransaction(): ConstraintSystemTransaction

    fun buildCurrentSubstitutor(): TypeSubstitutorMarker

    fun currentStorage(): ConstraintStorage
}

// if runOperations return true, then this operation will be applied, and function return true
@OptIn(ExperimentalContracts::class)
inline fun ConstraintSystemBuilder.runTransaction(crossinline runOperations: ConstraintSystemOperation.() -> Boolean): Boolean {
    contract {
        callsInPlace(runOperations, InvocationKind.EXACTLY_ONCE)
    }
    val transactionState = prepareTransaction()

    // typeVariablesTransaction is clear
    if (runOperations()) {
        transactionState.closeTransaction()
        return true
    }

    transactionState.rollbackTransaction()
    return false
}

fun ConstraintSystemBuilder.addSubtypeConstraintIfCompatible(
    lowerType: KotlinTypeMarker,
    upperType: KotlinTypeMarker,
    position: ConstraintPosition
): Boolean = addConstraintIfCompatible(lowerType, upperType, position, ConstraintKind.LOWER)

fun ConstraintSystemBuilder.addEqualityConstraintIfCompatible(
    lowerType: KotlinTypeMarker,
    upperType: KotlinTypeMarker,
    position: ConstraintPosition
): Boolean = addConstraintIfCompatible(lowerType, upperType, position, ConstraintKind.EQUALITY)

private fun ConstraintSystemBuilder.addConstraintIfCompatible(
    lowerType: KotlinTypeMarker,
    upperType: KotlinTypeMarker,
    position: ConstraintPosition,
    kind: ConstraintKind
): Boolean = runTransaction {
    if (!hasContradiction) {
        when (kind) {
            ConstraintKind.LOWER -> addSubtypeConstraint(lowerType, upperType, position)
            ConstraintKind.UPPER -> addSubtypeConstraint(upperType, lowerType, position)
            ConstraintKind.EQUALITY -> addEqualityConstraint(lowerType, upperType, position)
        }
    }
    !hasContradiction
}

fun ConstraintSystemBuilder.isSubtypeConstraintCompatible(
    lowerType: KotlinTypeMarker,
    upperType: KotlinTypeMarker,
): Boolean = isConstraintCompatible(lowerType, upperType, ConstraintKind.LOWER)

private fun ConstraintSystemBuilder.isConstraintCompatible(
    lowerType: KotlinTypeMarker,
    upperType: KotlinTypeMarker,
    kind: ConstraintKind
): Boolean {
    var isCompatible = false
    runTransaction {
        if (!hasContradiction) {
            // the type of position is irrelevant since the constraint is always rolled back.
            val position = SimpleConstraintSystemConstraintPosition
            when (kind) {
                ConstraintKind.LOWER -> addSubtypeConstraint(lowerType, upperType, position)
                ConstraintKind.UPPER -> addSubtypeConstraint(upperType, lowerType, position)
                ConstraintKind.EQUALITY -> addEqualityConstraint(lowerType, upperType, position)
            }
        }
        isCompatible = !hasContradiction
        false
    }
    return isCompatible
}
