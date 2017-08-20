/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.resolve.calls.inference.components

import org.jetbrains.kotlin.types.TypeApproximator
import org.jetbrains.kotlin.types.TypeApproximatorConfiguration
import org.jetbrains.kotlin.resolve.calls.inference.model.*
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.CaptureStatus
import org.jetbrains.kotlin.types.checker.NewCapturedType
import org.jetbrains.kotlin.types.checker.NewCapturedTypeConstructor
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection
import org.jetbrains.kotlin.types.typeUtil.contains
import org.jetbrains.kotlin.utils.SmartSet
import org.jetbrains.kotlin.utils.addIfNotNull
import java.util.*

// todo problem: intersection types in constrains: A <: Number, B <: Inv<A & Any> =>? B <: Inv<out Number & Any>
class ConstraintIncorporator(val typeApproximator: TypeApproximator) {

    interface Context {
        val allTypeVariablesWithConstraints: Collection<VariableWithConstraints>

        // if such type variable is fixed then it is error
        fun getTypeVariable(typeConstructor: TypeConstructor): NewTypeVariable?

        fun getConstraintsForVariable(typeVariable: NewTypeVariable): Collection<Constraint>

        fun addNewIncorporatedConstraint(lowerType: UnwrappedType, upperType: UnwrappedType, position: IncorporationConstraintPosition)
    }

    // \alpha is typeVariable, \beta -- other type variable registered in ConstraintStorage
    fun incorporate(c: Context, typeVariable: NewTypeVariable, constraint: Constraint, position: IncorporationConstraintPosition) {
        // we shouldn't incorporate recursive constraint -- It is too dangerous
        if (constraint.type.contains { it.constructor == typeVariable.freshTypeConstructor }) return

        directWithVariable(c, typeVariable, constraint, position)
        otherInsideMyConstraint(c, typeVariable, constraint, position)
        insideOtherConstraint(c, typeVariable, constraint, position)
    }

    // A <:(=) \alpha <:(=) B => A <: B
    private fun directWithVariable(c: Context, typeVariable: NewTypeVariable, constraint: Constraint, position: IncorporationConstraintPosition) {
        // \alpha <: constraint.type
        if (constraint.kind != ConstraintKind.LOWER) {
            c.getConstraintsForVariable(typeVariable).forEach {
                if (it.kind != ConstraintKind.UPPER) {
                    c.addNewIncorporatedConstraint(it.type, constraint.type, position)
                }
            }
        }

        // constraint.type <: \alpha
        if (constraint.kind != ConstraintKind.UPPER) {
            c.getConstraintsForVariable(typeVariable).forEach {
                if (it.kind != ConstraintKind.LOWER) {
                    c.addNewIncorporatedConstraint(constraint.type, it.type, position)
                }
            }
        }
    }

    // \alpha <: Inv<\beta>, \beta <: Number => \alpha <: Inv<out Number>
    private fun otherInsideMyConstraint(c: Context, typeVariable: NewTypeVariable, constraint: Constraint, position: IncorporationConstraintPosition) {
        val otherInMyConstraint = SmartSet.create<NewTypeVariable>()
        constraint.type.contains {
            otherInMyConstraint.addIfNotNull(c.getTypeVariable(it.constructor))
            false
        }

        for (otherTypeVariable in otherInMyConstraint) {
            // to avoid ConcurrentModificationException
            val otherConstraints = ArrayList(c.getConstraintsForVariable(otherTypeVariable))
            for (otherConstraint in otherConstraints) {
                generateNewConstraint(c, typeVariable, constraint, otherTypeVariable, otherConstraint, position)
            }
        }
    }

    // \alpha <: Number, \beta <: Inv<\alpha> => \beta <: Inv<out Number>
    private fun insideOtherConstraint(c: Context, typeVariable: NewTypeVariable, constraint: Constraint, position: IncorporationConstraintPosition) {
        for (typeVariableWithConstraint in c.allTypeVariablesWithConstraints) {
            val constraintsWhichConstraintMyVariable = typeVariableWithConstraint.constraints.filter {
                it.type.contains { it.constructor == typeVariable.freshTypeConstructor }
            }
            constraintsWhichConstraintMyVariable.forEach {
                generateNewConstraint(c, typeVariableWithConstraint.typeVariable, it, typeVariable, constraint, position)
            }
        }
    }

    private fun generateNewConstraint(
            c: Context,
            targetVariable: NewTypeVariable,
            baseConstraint: Constraint,
            otherVariable: NewTypeVariable,
            otherConstraint: Constraint,
            position: IncorporationConstraintPosition
    ) {
        val typeForApproximation = when (otherConstraint.kind) {
            ConstraintKind.EQUALITY -> {
                baseConstraint.type.substitute(otherVariable, otherConstraint.type)
            }
            ConstraintKind.UPPER -> {
                val newCapturedTypeConstructor = NewCapturedTypeConstructor(TypeProjectionImpl(Variance.OUT_VARIANCE, otherConstraint.type),
                                                                            listOf(otherConstraint.type))
                val temporaryCapturedType = NewCapturedType(CaptureStatus.FOR_INCORPORATION,
                                                            newCapturedTypeConstructor,
                                                            lowerType = null)
                baseConstraint.type.substitute(otherVariable, temporaryCapturedType)
            }
            ConstraintKind.LOWER -> {
                val newCapturedTypeConstructor = NewCapturedTypeConstructor(TypeProjectionImpl(Variance.IN_VARIANCE, otherConstraint.type),
                                                                            emptyList())
                val temporaryCapturedType = NewCapturedType(CaptureStatus.FOR_INCORPORATION,
                                                            newCapturedTypeConstructor,
                                                            lowerType = otherConstraint.type)
                baseConstraint.type.substitute(otherVariable, temporaryCapturedType)
            }
        }

        if (baseConstraint.kind != ConstraintKind.UPPER) {
            c.addNewIncorporatedConstraint(approximateCapturedTypes(typeForApproximation, toSuper = false), targetVariable.defaultType, position)
        }
        if (baseConstraint.kind != ConstraintKind.LOWER) {
            c.addNewIncorporatedConstraint(targetVariable.defaultType, approximateCapturedTypes(typeForApproximation, toSuper = true), position)
        }
    }

    private fun UnwrappedType.substitute(typeVariable: NewTypeVariable, value: UnwrappedType): UnwrappedType {
        val substitutor = TypeSubstitutor.create(mapOf(typeVariable.freshTypeConstructor to value.asTypeProjection()))
        val type = substitutor.substitute(this, Variance.INVARIANT) ?: error("Impossible to substitute in $this: $typeVariable -> $value")
        return type.unwrap()
    }

    private fun approximateCapturedTypes(type: UnwrappedType, toSuper: Boolean): UnwrappedType =
            if (toSuper) typeApproximator.approximateToSuperType(type, TypeApproximatorConfiguration.IncorporationConfiguration) ?: type
            else typeApproximator.approximateToSubType(type, TypeApproximatorConfiguration.IncorporationConfiguration) ?: type


}