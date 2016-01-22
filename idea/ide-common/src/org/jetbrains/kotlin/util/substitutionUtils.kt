/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.types.substitutions

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.TypeCheckingProcedure
import java.util.LinkedHashMap

fun getTypeSubstitution(baseType: KotlinType, derivedType: KotlinType): LinkedHashMap<TypeConstructor, TypeProjection>? {
    val substitutedType = TypeCheckingProcedure.findCorrespondingSupertype(derivedType, baseType) ?: return null

    val substitution = LinkedHashMap<TypeConstructor, TypeProjection>(substitutedType.arguments.size)
    for ((param, arg) in baseType.constructor.parameters.zip(substitutedType.arguments)) {
        substitution[param.typeConstructor] = arg
    }

    return substitution
}

fun getCallableSubstitution(
        baseCallable: CallableDescriptor,
        derivedCallable: CallableDescriptor
): MutableMap<TypeConstructor, TypeProjection>? {
    val baseClass = baseCallable.containingDeclaration as? ClassDescriptor ?: return null
    val derivedClass = derivedCallable.containingDeclaration as? ClassDescriptor ?: return null
    val substitution = getTypeSubstitution(baseClass.defaultType, derivedClass.defaultType) ?: return null

    for ((baseParam, derivedParam) in baseCallable.typeParameters.zip(derivedCallable.typeParameters)) {
        substitution[baseParam.typeConstructor] = TypeProjectionImpl(derivedParam.defaultType)
    }

    return substitution
}

fun getCallableSubstitutor(
        baseCallable: CallableDescriptor,
        derivedCallable: CallableDescriptor
): TypeSubstitutor? {
    return getCallableSubstitution(baseCallable, derivedCallable)?.let { TypeSubstitutor.create(it) }
}

fun getTypeSubstitutor(baseType: KotlinType, derivedType: KotlinType): TypeSubstitutor? {
    return getTypeSubstitution(baseType, derivedType)?.let { TypeSubstitutor.create(it) }
}
