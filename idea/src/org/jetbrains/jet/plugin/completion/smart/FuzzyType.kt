/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.completion.smart

import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor
import org.jetbrains.jet.lang.descriptors.CallableDescriptor
import org.jetbrains.jet.utils.addIfNotNull
import java.util.HashSet
import org.jetbrains.jet.lang.resolve.calls.inference.ConstraintSystemImpl
import java.util.LinkedHashMap
import org.jetbrains.jet.lang.types.Variance
import org.jetbrains.jet.lang.resolve.calls.inference.ConstraintPosition
import org.jetbrains.jet.lang.resolve.calls.inference.ConstraintsUtil
import org.jetbrains.jet.plugin.util.makeNotNullable
import org.jetbrains.jet.plugin.util.makeNullable
import org.jetbrains.jet.lang.types.TypeSubstitutor
import org.jetbrains.jet.lang.types.typeUtil.isSubtypeOf

fun CallableDescriptor.fuzzyReturnType(): FuzzyType? {
    val returnType = getReturnType() ?: return null
    return FuzzyType(returnType, getTypeParameters())
}

//TODO: replace code in extensionsUtils.kt with use of FuzzyType
fun CallableDescriptor.fuzzyExtensionReceiverType(): FuzzyType? {
    val receiverParameter = getExtensionReceiverParameter()
    return if (receiverParameter != null) FuzzyType(receiverParameter.getType(), getTypeParameters()) else null
}

fun FuzzyType.makeNotNullable() = FuzzyType(type.makeNotNullable(), freeParameters)
fun FuzzyType.makeNullable() = FuzzyType(type.makeNullable(), freeParameters)
fun FuzzyType.isNullable() = type.isNullable()

class FuzzyType(
        val type: JetType,
        val freeParameters: Collection<TypeParameterDescriptor>
) {
    private val usedTypeParameters: HashSet<TypeParameterDescriptor>? = if (freeParameters.isNotEmpty()) HashSet() else null

    ;{
        usedTypeParameters?.addUsedTypeParameters(type)
    }

    private fun MutableSet<TypeParameterDescriptor>.addUsedTypeParameters(type: JetType) {
        addIfNotNull(type.getConstructor().getDeclarationDescriptor() as? TypeParameterDescriptor)

        for (argument in type.getArguments()) {
            addUsedTypeParameters(argument.getType())
        }
    }

    public fun matchedSubstitutor(expectedType: JetType): TypeSubstitutor? {
        if (type.isError()) return null
        if (usedTypeParameters == null || usedTypeParameters.isEmpty()) {
            return if (type.isSubtypeOf(expectedType)) TypeSubstitutor.EMPTY else null
        }

        val constraintSystem = ConstraintSystemImpl()
        val typeVariables = LinkedHashMap<TypeParameterDescriptor, Variance>()
        for (typeParameter in freeParameters) {
            if (typeParameter in usedTypeParameters) {
                typeVariables[typeParameter] = Variance.INVARIANT
            }
        }
        constraintSystem.registerTypeVariables(typeVariables)

        constraintSystem.addSubtypeConstraint(type, expectedType, ConstraintPosition.SPECIAL/*TODO?*/)
        if (constraintSystem.getStatus().isSuccessful() && ConstraintsUtil.checkBoundsAreSatisfied(constraintSystem, true)) {
            val substitutor = constraintSystem.getResultingSubstitutor()
            val substitutedType = substitutor.substitute(type, Variance.INVARIANT/*TODO?*/)
            return if (substitutedType.isSubtypeOf(expectedType)) substitutor else null
        }
        else {
            return null
        }
    }
}
