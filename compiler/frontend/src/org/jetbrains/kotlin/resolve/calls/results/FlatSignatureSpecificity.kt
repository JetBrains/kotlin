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

package org.jetbrains.kotlin.resolve.calls.results

import org.jetbrains.kotlin.resolve.calls.inference.CallHandle
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystem
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilderImpl
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.valueParameterPosition
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker

interface SpecificityComparisonCallbacks<T> {
    fun isNotLessSpecificSignature(signature1: FlatSignature<T>, signature2: FlatSignature<T>): Boolean?
    fun isTypeNotLessSpecific(type1: KotlinType, type2: KotlinType): Boolean?
}

fun <T> isSignatureNotLessSpecific(
        signature1: FlatSignature<T>,
        signature2: FlatSignature<T>,
        callHandle: CallHandle,
        callbacks: SpecificityComparisonCallbacks<T>
): Boolean {
    callbacks.isNotLessSpecificSignature(signature1, signature2)?.let { return it }

    val typeParameters = signature2.typeParameters
    val constraintSystemBuilder: ConstraintSystem.Builder = ConstraintSystemBuilderImpl()
    var numConstraints = 0
    val typeSubstitutor = constraintSystemBuilder.registerTypeVariables(callHandle, typeParameters)

    for ((type1, type2) in signature1.valueParameterTypes.zip(signature2.valueParameterTypes)) {
        if (type1 == null || type2 == null) continue

        if (isDefinitelyLessSpecificByTypeSpecificity(type1, type2)) {
            return false
        }

        if (typeParameters.isEmpty() || !TypeUtils.dependsOnTypeParameters(type2, typeParameters)) {
            if (!KotlinTypeChecker.DEFAULT.isSubtypeOf(type1, type2)) {
                callbacks.isTypeNotLessSpecific(type1, type2)?.let { if (!it) return false }
            }
        }
        else {
            val constraintPosition = valueParameterPosition(numConstraints++)
            val substitutedType2 = typeSubstitutor.safeSubstitute(type2, Variance.INVARIANT)
            constraintSystemBuilder.addSubtypeConstraint(type1, substitutedType2, constraintPosition)
        }
    }

    if (numConstraints > 0) {
        constraintSystemBuilder.fixVariables()
        val constraintSystem = constraintSystemBuilder.build()
        if (constraintSystem.status.hasContradiction()) {
            return false
        }
    }

    return true
}

private fun isDefinitelyLessSpecificByTypeSpecificity(specific: KotlinType, general: KotlinType): Boolean {
    val sThanG = specific.getSpecificityRelationTo(general)
    val gThanS = general.getSpecificityRelationTo(specific)
    return sThanG == Specificity.Relation.LESS_SPECIFIC &&
           gThanS != Specificity.Relation.LESS_SPECIFIC
}