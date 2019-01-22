/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.inference.components

import org.jetbrains.kotlin.resolve.calls.inference.model.Constraint
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintKind
import org.jetbrains.kotlin.resolve.calls.inference.model.NewTypeVariable
import org.jetbrains.kotlin.resolve.calls.inference.model.VariableWithConstraints
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.NewCapturedType
import org.jetbrains.kotlin.types.checker.NewCapturedTypeConstructor
import org.jetbrains.kotlin.types.model.CaptureStatus
import org.jetbrains.kotlin.types.typeUtil.contains
import org.jetbrains.kotlin.utils.SmartSet
import org.jetbrains.kotlin.utils.addIfNotNull
import java.util.*

// todo problem: intersection types in constrains: A <: Number, B <: Inv<A & Any> =>? B <: Inv<out Number & Any>
class ConstraintIncorporator(
    val typeApproximator: TypeApproximator,
    val trivialConstraintTypeInferenceOracle: TrivialConstraintTypeInferenceOracle
) {

    interface Context {
        val allTypeVariablesWithConstraints: Collection<VariableWithConstraints>

        // if such type variable is fixed then it is error
        fun getTypeVariable(typeConstructor: TypeConstructor): NewTypeVariable?

        fun getConstraintsForVariable(typeVariable: NewTypeVariable): Collection<Constraint>

        fun addNewIncorporatedConstraint(lowerType: UnwrappedType, upperType: UnwrappedType)
    }

    // \alpha is typeVariable, \beta -- other type variable registered in ConstraintStorage
    fun incorporate(c: Context, typeVariable: NewTypeVariable, constraint: Constraint) {
        // we shouldn't incorporate recursive constraint -- It is too dangerous
        if (constraint.type.contains { it.constructor == typeVariable.freshTypeConstructor }) return

        directWithVariable(c, typeVariable, constraint)
        otherInsideMyConstraint(c, typeVariable, constraint)
        insideOtherConstraint(c, typeVariable, constraint)
    }

    // A <:(=) \alpha <:(=) B => A <: B
    private fun directWithVariable(c: Context, typeVariable: NewTypeVariable, constraint: Constraint) {
        // \alpha <: constraint.type
        if (constraint.kind != ConstraintKind.LOWER) {
            c.getConstraintsForVariable(typeVariable).forEach {
                if (it.kind != ConstraintKind.UPPER) {
                    c.addNewIncorporatedConstraint(it.type, constraint.type)
                }
            }
        }

        // constraint.type <: \alpha
        if (constraint.kind != ConstraintKind.UPPER) {
            c.getConstraintsForVariable(typeVariable).forEach {
                if (it.kind != ConstraintKind.LOWER) {
                    c.addNewIncorporatedConstraint(constraint.type, it.type)
                }
            }
        }
    }

    // \alpha <: Inv<\beta>, \beta <: Number => \alpha <: Inv<out Number>
    private fun otherInsideMyConstraint(c: Context, typeVariable: NewTypeVariable, constraint: Constraint) {
        val otherInMyConstraint = SmartSet.create<NewTypeVariable>()
        constraint.type.contains {
            otherInMyConstraint.addIfNotNull(c.getTypeVariable(it.constructor))
            false
        }

        for (otherTypeVariable in otherInMyConstraint) {
            // to avoid ConcurrentModificationException
            val otherConstraints = ArrayList(c.getConstraintsForVariable(otherTypeVariable))
            for (otherConstraint in otherConstraints) {
                generateNewConstraint(c, typeVariable, constraint, otherTypeVariable, otherConstraint)
            }
        }
    }

    // \alpha <: Number, \beta <: Inv<\alpha> => \beta <: Inv<out Number>
    private fun insideOtherConstraint(c: Context, typeVariable: NewTypeVariable, constraint: Constraint) {
        for (typeVariableWithConstraint in c.allTypeVariablesWithConstraints) {
            val constraintsWhichConstraintMyVariable = typeVariableWithConstraint.constraints.filter {
                it.type.contains { it.constructor == typeVariable.freshTypeConstructor }
            }
            constraintsWhichConstraintMyVariable.forEach {
                generateNewConstraint(c, typeVariableWithConstraint.typeVariable, it, typeVariable, constraint)
            }
        }
    }

    private fun generateNewConstraint(
        c: Context,
        targetVariable: NewTypeVariable,
        baseConstraint: Constraint,
        otherVariable: NewTypeVariable,
        otherConstraint: Constraint
    ) {
        val baseConstraintType = baseConstraint.type
        val typeForApproximation = when (otherConstraint.kind) {
            ConstraintKind.EQUALITY -> {
                baseConstraintType.substituteTypeVariable(otherVariable, otherConstraint.type)
            }
            ConstraintKind.UPPER -> {
                val newCapturedTypeConstructor = NewCapturedTypeConstructor(
                    TypeProjectionImpl(Variance.OUT_VARIANCE, otherConstraint.type),
                    listOf(otherConstraint.type)
                )
                val temporaryCapturedType = NewCapturedType(
                    CaptureStatus.FOR_INCORPORATION,
                    newCapturedTypeConstructor,
                    lowerType = null
                )
                baseConstraintType.substituteTypeVariable(otherVariable, temporaryCapturedType)
            }
            ConstraintKind.LOWER -> {
                val newCapturedTypeConstructor = NewCapturedTypeConstructor(
                    TypeProjectionImpl(Variance.IN_VARIANCE, otherConstraint.type),
                    emptyList()
                )
                val temporaryCapturedType = NewCapturedType(
                    CaptureStatus.FOR_INCORPORATION,
                    newCapturedTypeConstructor,
                    lowerType = otherConstraint.type
                )
                baseConstraintType.substituteTypeVariable(otherVariable, temporaryCapturedType)
            }
        }

        if (baseConstraint.kind != ConstraintKind.UPPER) {
            val generatedConstraintType = approximateCapturedTypes(typeForApproximation, toSuper = false)
            if (!trivialConstraintTypeInferenceOracle.isGeneratedConstraintTrivial(otherConstraint, generatedConstraintType)) {
                c.addNewIncorporatedConstraint(generatedConstraintType, targetVariable.defaultType)
            }
        }
        if (baseConstraint.kind != ConstraintKind.LOWER) {
            val generatedConstraintType = approximateCapturedTypes(typeForApproximation, toSuper = true)
            if (!trivialConstraintTypeInferenceOracle.isGeneratedConstraintTrivial(otherConstraint, generatedConstraintType)) {
                c.addNewIncorporatedConstraint(targetVariable.defaultType, generatedConstraintType)
            }
        }
    }

    private fun approximateCapturedTypes(type: UnwrappedType, toSuper: Boolean): UnwrappedType =
        if (toSuper) typeApproximator.approximateToSuperType(type, TypeApproximatorConfiguration.IncorporationConfiguration) ?: type
        else typeApproximator.approximateToSubType(type, TypeApproximatorConfiguration.IncorporationConfiguration) ?: type
}
