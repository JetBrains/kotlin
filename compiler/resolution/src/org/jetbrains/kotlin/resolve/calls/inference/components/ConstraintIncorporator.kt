/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.inference.components

import org.jetbrains.kotlin.resolve.calls.inference.model.*
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.model.*
import org.jetbrains.kotlin.utils.SmartSet
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.util.*

// todo problem: intersection types in constrains: A <: Number, B <: Inv<A & Any> =>? B <: Inv<out Number & Any>
class ConstraintIncorporator(
    val typeApproximator: AbstractTypeApproximator,
    val trivialConstraintTypeInferenceOracle: TrivialConstraintTypeInferenceOracle
) {

    interface Context : TypeSystemInferenceExtensionContext {
        val allTypeVariablesWithConstraints: Collection<VariableWithConstraints>

        // if such type variable is fixed then it is error
        fun getTypeVariable(typeConstructor: TypeConstructorMarker): TypeVariableMarker?

        fun getConstraintsForVariable(typeVariable: TypeVariableMarker): Collection<Constraint>

        fun addNewIncorporatedConstraint(lowerType: KotlinTypeMarker, upperType: KotlinTypeMarker)

        fun addNewIncorporatedConstraint(typeVariable: TypeVariableMarker, type: KotlinTypeMarker, constraintContext: ConstraintContext)
    }

    // \alpha is typeVariable, \beta -- other type variable registered in ConstraintStorage
    fun incorporate(c: Context, typeVariable: TypeVariableMarker, constraint: Constraint) {
        // we shouldn't incorporate recursive constraint -- It is too dangerous
        with(c) {
            if (constraint.type.contains { it.typeConstructor() == typeVariable.freshTypeConstructor() }) return
        }

        c.directWithVariable(typeVariable, constraint)
        c.otherInsideMyConstraint(typeVariable, constraint)
        c.insideOtherConstraint(typeVariable, constraint)
    }

    // A <:(=) \alpha <:(=) B => A <: B
    private fun Context.directWithVariable(
        typeVariable: TypeVariableMarker,
        constraint: Constraint
    ) {
        // \alpha <: constraint.type
        if (constraint.kind != ConstraintKind.LOWER) {
            getConstraintsForVariable(typeVariable).forEach {
                if (it.kind != ConstraintKind.UPPER) {
                    addNewIncorporatedConstraint(it.type, constraint.type)
                }
            }
        }

        // constraint.type <: \alpha
        if (constraint.kind != ConstraintKind.UPPER) {
            getConstraintsForVariable(typeVariable).forEach {
                if (it.kind != ConstraintKind.LOWER) {
                    addNewIncorporatedConstraint(constraint.type, it.type)
                }
            }
        }
    }

    // \alpha <: Inv<\beta>, \beta <: Number => \alpha <: Inv<out Number>
    private fun Context.otherInsideMyConstraint(
        typeVariable: TypeVariableMarker,
        constraint: Constraint
    ) {
        val otherInMyConstraint = SmartSet.create<TypeVariableMarker>()
        constraint.type.contains {
            otherInMyConstraint.addIfNotNull(this.getTypeVariable(it.typeConstructor()))
            false
        }

        for (otherTypeVariable in otherInMyConstraint) {
            // to avoid ConcurrentModificationException
            val otherConstraints = ArrayList(this.getConstraintsForVariable(otherTypeVariable))
            for (otherConstraint in otherConstraints) {
                generateNewConstraint(typeVariable, constraint, otherTypeVariable, otherConstraint)
            }
        }
    }

    // \alpha <: Number, \beta <: Inv<\alpha> => \beta <: Inv<out Number>
    private fun Context.insideOtherConstraint(
        typeVariable: TypeVariableMarker,
        constraint: Constraint
    ) {
        for (typeVariableWithConstraint in this@insideOtherConstraint.allTypeVariablesWithConstraints) {
            val constraintsWhichConstraintMyVariable = typeVariableWithConstraint.constraints.filter {
                it.type.contains { it.typeConstructor() == typeVariable.freshTypeConstructor() }
            }
            constraintsWhichConstraintMyVariable.forEach {
                generateNewConstraint(typeVariableWithConstraint.typeVariable, it, typeVariable, constraint)
            }
        }
    }

    private fun Context.generateNewConstraint(
        targetVariable: TypeVariableMarker,
        baseConstraint: Constraint,
        otherVariable: TypeVariableMarker,
        otherConstraint: Constraint
    ) {

        val baseConstraintType = baseConstraint.type

        val typeForApproximation = when (otherConstraint.kind) {
            ConstraintKind.EQUALITY -> {
                baseConstraintType.substitute(this, otherVariable, otherConstraint.type)
            }
            ConstraintKind.UPPER -> {
                val temporaryCapturedType = createCapturedType(
                    createTypeArgument(otherConstraint.type, TypeVariance.OUT),
                    listOf(otherConstraint.type),
                    null,
                    CaptureStatus.FOR_INCORPORATION
                )
                baseConstraintType.substitute(this, otherVariable, temporaryCapturedType)
            }
            ConstraintKind.LOWER -> {
                val temporaryCapturedType = createCapturedType(
                    createTypeArgument(otherConstraint.type, TypeVariance.IN),
                    emptyList(),
                    otherConstraint.type,
                    CaptureStatus.FOR_INCORPORATION
                )

                baseConstraintType.substitute(this, otherVariable, temporaryCapturedType)
            }
        }

        if (baseConstraint.kind != ConstraintKind.UPPER) {
            val generatedConstraintType = approximateCapturedTypes(typeForApproximation, toSuper = false)
            addNewConstraint(targetVariable, baseConstraint, otherVariable, otherConstraint, generatedConstraintType, isSubtype = true)
        }
        if (baseConstraint.kind != ConstraintKind.LOWER) {
            val generatedConstraintType = approximateCapturedTypes(typeForApproximation, toSuper = true)
            addNewConstraint(targetVariable, baseConstraint, otherVariable, otherConstraint, generatedConstraintType, isSubtype = false)
        }
    }

    private fun Context.addNewConstraint(
        targetVariable: TypeVariableMarker,
        baseConstraint: Constraint,
        otherVariable: TypeVariableMarker,
        otherConstraint: Constraint,
        newConstraint: KotlinTypeMarker,
        isSubtype: Boolean
    ) {
        if (targetVariable in getNestedTypeVariables(newConstraint)) return

        val isUsefulForNullabilityConstraint =
            isPotentialUsefulNullabilityConstraint(newConstraint, otherConstraint.type, otherConstraint.kind)

        if (!isUsefulForNullabilityConstraint && !containsConstrainingTypeWithoutProjection(newConstraint, otherConstraint)) return
        if (trivialConstraintTypeInferenceOracle.isGeneratedConstraintTrivial(
                baseConstraint, otherConstraint, newConstraint, isSubtype
            )
        ) return

        val derivedFrom = (baseConstraint.derivedFrom + otherConstraint.derivedFrom).toMutableSet()
        if (otherVariable in derivedFrom) return

        derivedFrom.add(otherVariable)

        val kind = if (isSubtype) ConstraintKind.LOWER else ConstraintKind.UPPER

        val inputTypePosition = baseConstraint.position.from.safeAs<OnlyInputTypeConstraintPosition>()

        val isNullabilityConstraint = isUsefulForNullabilityConstraint && newConstraint.isNullableNothing()
        val constraintContext = ConstraintContext(kind, derivedFrom, inputTypePosition, isNullabilityConstraint)

        addNewIncorporatedConstraint(targetVariable, newConstraint, constraintContext)
    }

    fun Context.containsConstrainingTypeWithoutProjection(
        newConstraint: KotlinTypeMarker,
        otherConstraint: Constraint
    ): Boolean {
        return getNestedArguments(newConstraint).any {
            it.getType().typeConstructor() == otherConstraint.type.typeConstructor() && it.getVariance() == TypeVariance.INV
        }
    }

    private fun Context.isPotentialUsefulNullabilityConstraint(
        newConstraint: KotlinTypeMarker,
        otherConstraint: KotlinTypeMarker,
        kind: ConstraintKind
    ): Boolean {
        val otherConstraintCanAddNullabilityToNewOne =
            !newConstraint.isNullableType() && otherConstraint.isNullableType() && kind == ConstraintKind.LOWER
        val newConstraintCanAddNullabilityToOtherOne =
            newConstraint.isNullableType() && !otherConstraint.isNullableType() && kind == ConstraintKind.UPPER

        return otherConstraintCanAddNullabilityToNewOne || newConstraintCanAddNullabilityToOtherOne
    }

    fun Context.getNestedTypeVariables(type: KotlinTypeMarker): List<TypeVariableMarker> =
        getNestedArguments(type).mapNotNull { getTypeVariable(it.getType().typeConstructor()) }


    private fun KotlinTypeMarker.substitute(c: Context, typeVariable: TypeVariableMarker, value: KotlinTypeMarker): KotlinTypeMarker {
        val substitutor = c.typeSubstitutorByTypeConstructor(mapOf(typeVariable.freshTypeConstructor(c) to value))
        return substitutor.safeSubstitute(c, this)
    }


    private fun approximateCapturedTypes(type: KotlinTypeMarker, toSuper: Boolean): KotlinTypeMarker =
        if (toSuper) typeApproximator.approximateToSuperType(type, TypeApproximatorConfiguration.IncorporationConfiguration) ?: type
        else typeApproximator.approximateToSubType(type, TypeApproximatorConfiguration.IncorporationConfiguration) ?: type
}

private fun TypeSystemInferenceExtensionContext.getNestedArguments(type: KotlinTypeMarker): List<TypeArgumentMarker> {
    val result = ArrayList<TypeArgumentMarker>()
    val stack = ArrayDeque<TypeArgumentMarker>()

    when (type) {
        is FlexibleType -> {
            stack.push(createTypeArgument(type.lowerBound, TypeVariance.INV))
            stack.push(createTypeArgument(type.upperBound, TypeVariance.INV))
        }
        else -> stack.push(createTypeArgument(type, TypeVariance.INV))
    }

    stack.push(createTypeArgument(type, TypeVariance.INV))

    val addArgumentsToStack = { projectedType: KotlinTypeMarker ->
        for (argumentIndex in 0 until projectedType.argumentsCount()) {
            stack.add(projectedType.getArgument(argumentIndex))
        }
    }

    while (!stack.isEmpty()) {
        val typeProjection = stack.pop()
        if (typeProjection.isStarProjection()) continue

        result.add(typeProjection)

        when (val projectedType = typeProjection.getType()) {
            is FlexibleType -> {
                addArgumentsToStack(projectedType.lowerBound)
                addArgumentsToStack(projectedType.upperBound)
            }
            else -> addArgumentsToStack(projectedType)
        }
    }
    return result
}
