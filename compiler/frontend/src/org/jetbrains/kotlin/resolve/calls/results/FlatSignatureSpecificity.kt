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

interface SpecificityComparisonCallbacks {
    fun isNonSubtypeNotLessSpecific(specific: KotlinType, general: KotlinType): Boolean
}

fun <T> isSignatureNotLessSpecific(
        specific: FlatSignature<T>,
        general: FlatSignature<T>,
        callbacks: SpecificityComparisonCallbacks,
        callHandle: CallHandle = CallHandle.NONE
): Boolean {
    if (specific.hasExtensionReceiver != general.hasExtensionReceiver) return false
    if (specific.valueParameterTypes.size != general.valueParameterTypes.size) return false

    val typeParameters = general.typeParameters
    val constraintSystemBuilder: ConstraintSystem.Builder = ConstraintSystemBuilderImpl()
    val typeSubstitutor = constraintSystemBuilder.registerTypeVariables(callHandle, typeParameters)

    var numConstraints = 0
    for ((specificType, generalType) in specific.valueParameterTypes.zip(general.valueParameterTypes)) {
        if (specificType == null || generalType == null) continue

        if (isDefinitelyLessSpecificByTypeSpecificity(specificType, generalType)) {
            return false
        }

        if (typeParameters.isEmpty() || !TypeUtils.dependsOnTypeParameters(generalType, typeParameters)) {
            if (!KotlinTypeChecker.DEFAULT.isSubtypeOf(specificType, generalType)) {
                if (!callbacks.isNonSubtypeNotLessSpecific(specificType, generalType)) {
                    return false
                }
            }
        }
        else {
            val substitutedGeneralType = typeSubstitutor.safeSubstitute(generalType, Variance.INVARIANT)
            constraintSystemBuilder.addSubtypeConstraint(specificType, substitutedGeneralType, valueParameterPosition(numConstraints++))
        }
    }

    constraintSystemBuilder.fixVariables()
    val constraintSystem = constraintSystemBuilder.build()
    return !constraintSystem.status.hasContradiction()
}

private fun isDefinitelyLessSpecificByTypeSpecificity(specific: KotlinType, general: KotlinType): Boolean {
    val sThanG = specific.getSpecificityRelationTo(general)
    val gThanS = general.getSpecificityRelationTo(specific)
    return sThanG == Specificity.Relation.LESS_SPECIFIC &&
           gThanS != Specificity.Relation.LESS_SPECIFIC
}