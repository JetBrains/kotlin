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

public fun getTypeSubstitution(baseType: JetType, derivedType: JetType): LinkedHashMap<TypeConstructor, TypeProjection>? {
    val substitutedType = TypeCheckingProcedure.findCorrespondingSupertype(derivedType, baseType) ?: return null

    val substitution = LinkedHashMap<TypeConstructor, TypeProjection>(substitutedType.getArguments().size())
    for ((param, arg) in baseType.getConstructor().getParameters() zip substitutedType.getArguments()) {
        substitution[param.getTypeConstructor()] = arg
    }

    return substitution
}

public fun getCallableSubstitution(
        baseCallable: CallableDescriptor,
        derivedCallable: CallableDescriptor
): MutableMap<TypeConstructor, TypeProjection>? {
    val baseClass = baseCallable.getContainingDeclaration() as? ClassDescriptor ?: return null
    val derivedClass = derivedCallable.getContainingDeclaration() as? ClassDescriptor ?: return null
    val substitution = getTypeSubstitution(baseClass.getDefaultType(), derivedClass.getDefaultType()) ?: return null

    for ((baseParam, derivedParam) in baseCallable.getTypeParameters() zip derivedCallable.getTypeParameters()) {
        substitution[baseParam.getTypeConstructor()] = TypeProjectionImpl(derivedParam.getDefaultType())
    }

    return substitution
}

public fun getCallableSubstitutor(
        baseCallable: CallableDescriptor,
        derivedCallable: CallableDescriptor
): TypeSubstitutor? {
    return getCallableSubstitution(baseCallable, derivedCallable)?.let { TypeSubstitutor.create(it) }
}

public fun getTypeSubstitutor(baseType: JetType, derivedType: JetType): TypeSubstitutor? {
    return getTypeSubstitution(baseType, derivedType)?.let { TypeSubstitutor.create(it) }
}
