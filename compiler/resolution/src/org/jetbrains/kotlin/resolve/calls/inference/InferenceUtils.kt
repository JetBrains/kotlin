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

package org.jetbrains.kotlin.resolve.calls.inference

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.resolve.calls.inference.components.NewTypeSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.components.NewTypeSubstitutorByConstructorMap
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage
import org.jetbrains.kotlin.resolve.calls.inference.model.NewConstraintSystemImpl
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.types.*

fun ConstraintStorage.buildCurrentSubstitutor() = NewTypeSubstitutorByConstructorMap(fixedTypeVariables.entries.associate {
    it.key to it.value
})

fun ConstraintStorage.buildResultingSubstitutor(): NewTypeSubstitutor {
    val currentSubstitutorMap = fixedTypeVariables.entries.associate {
        it.key to it.value
    }
    val uninferredSubstitutorMap = notFixedTypeVariables.entries.associate { (freshTypeConstructor, typeVariable) ->
        freshTypeConstructor to ErrorUtils.createErrorTypeWithCustomConstructor("Uninferred type", typeVariable.typeVariable.freshTypeConstructor)
    }

    return NewTypeSubstitutorByConstructorMap(currentSubstitutorMap + uninferredSubstitutorMap)
}

val CallableDescriptor.returnTypeOrNothing: UnwrappedType
    get() {
        returnType?.let { return it.unwrap() }

        return builtIns.nothingType
    }

fun TypeSubstitutor.substitute(type: UnwrappedType): UnwrappedType = safeSubstitute(type, Variance.INVARIANT).unwrap()

fun CallableDescriptor.substitute(substitutor: NewTypeSubstitutor): CallableDescriptor {
    val wrappedSubstitution = object : TypeSubstitution() {
        override fun get(key: KotlinType): TypeProjection? = null
        override fun prepareTopLevelType(topLevelType: KotlinType, position: Variance) = substitutor.safeSubstitute(topLevelType.unwrap())
    }
    return substitute(TypeSubstitutor.create(wrappedSubstitution))
}

fun CallableDescriptor.substituteAndApproximateCapturedTypes(substitutor: NewTypeSubstitutor): CallableDescriptor {
    val wrappedSubstitution = object : TypeSubstitution() {
        override fun get(key: KotlinType): TypeProjection? = null

        override fun prepareTopLevelType(topLevelType: KotlinType, position: Variance) =
                substitutor.safeSubstitute(topLevelType.unwrap()).let { substitutedType ->
                    TypeApproximator().approximateToSuperType(substitutedType, TypeApproximatorConfiguration.CapturedTypesApproximation) ?:
                    substitutedType
                }
    }

    return substitute(TypeSubstitutor.create(wrappedSubstitution))
}

internal fun <E> MutableList<E>.trimToSize(newSize: Int) = subList(newSize, size).clear()