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

package org.jetbrains.kotlin.load.java.components

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.load.java.components.MutabilityQualifier.MUTABLE
import org.jetbrains.kotlin.load.java.components.MutabilityQualifier.READ_ONLY
import org.jetbrains.kotlin.load.java.components.NullabilityQualifier.NOT_NULL
import org.jetbrains.kotlin.load.java.components.NullabilityQualifier.NULLABLE
import org.jetbrains.kotlin.platform.JavaToKotlinClassMap
import org.jetbrains.kotlin.types.*


fun JetType.enhance(qualifiers: JavaTypeQualifiers): JetType {
    val mutabilityEnhanced =
            if (this.isFlexible())
                this.flexibility().enhanceMutability(qualifiers.mutability)
            else this
    return mutabilityEnhanced.enhanceNullability(qualifiers.nullability)
}

private fun Flexibility.enhanceMutability(qualifier: MutabilityQualifier?): JetType {
    val mapping = JavaToKotlinClassMap.INSTANCE

    val (newLower, newUpper) = run {
        when (qualifier) {
            READ_ONLY -> {
                val lowerClass = TypeUtils.getClassDescriptor(lowerBound)
                if (lowerClass != null && mapping.isMutable(lowerClass)) {
                    return@run Pair(lowerBound.replaceClass(mapping.convertMutableToReadOnly(lowerClass)), upperBound)
                }
            }
            MUTABLE -> {
                val upperClass = TypeUtils.getClassDescriptor(upperBound)
                if (upperClass != null && mapping.isReadOnly(upperClass) ) {
                    return@run Pair(lowerBound, upperBound.replaceClass(mapping.convertReadOnlyToMutable(upperClass)))
                }
            }
        }
        return@run Pair(lowerBound, upperBound)
    }

    return DelegatingFlexibleType.create(newLower, newUpper, extraCapabilities)
}

private fun JetType.enhanceNullability(qualifier: NullabilityQualifier?): JetType {
    return when (qualifier) {
        NULLABLE -> TypeUtils.makeNullable(this)
        NOT_NULL -> TypeUtils.makeNotNullable(this)
        else -> this
    }
}

private fun JetType.replaceClass(newClass: ClassDescriptor): JetType {
    assert(newClass.getTypeConstructor().getParameters().size() == getArguments().size(),
           {"Can't replace type constructor ${getConstructor()} by ${newClass}: type parameter count does not match"})
    return JetTypeImpl(
            getAnnotations(),
            newClass.getTypeConstructor(),
            isMarkedNullable(),
            getArguments(),
            newClass.getMemberScope(getArguments())
    )
}