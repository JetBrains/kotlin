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

package org.jetbrains.kotlin.types


// To facilitate laziness, any KotlinType implementation may inherit from this trait,
// even if it turns out that the type an instance represents is not actually a type parameter
// (i.e. it is not derived from a type parameter), see isTypeParameter
interface CustomTypeParameter {
    val isTypeParameter: Boolean

    // Throws an exception when isTypeParameter == false
    fun substitutionResult(replacement: KotlinType): KotlinType
}

// That interface is needed to provide information about definitely not null
//   type parameters (e.g. from @NotNull annotation) to type system
interface NotNullTypeParameter : CustomTypeParameter

fun KotlinType.isCustomTypeParameter(): Boolean = (unwrap() as? CustomTypeParameter)?.isTypeParameter ?: false
fun KotlinType.getCustomTypeParameter(): CustomTypeParameter? =
    (unwrap() as? CustomTypeParameter)?.let {
        if (it.isTypeParameter) it else null
    }

interface SubtypingRepresentatives {
    val subTypeRepresentative: KotlinType
    val superTypeRepresentative: KotlinType

    fun sameTypeConstructor(type: KotlinType): Boolean
}

fun KotlinType.getSubtypeRepresentative(): KotlinType =
    (unwrap() as? SubtypingRepresentatives)?.subTypeRepresentative ?: this

fun KotlinType.getSupertypeRepresentative(): KotlinType =
    (unwrap() as? SubtypingRepresentatives)?.superTypeRepresentative ?: this

fun sameTypeConstructors(first: KotlinType, second: KotlinType): Boolean {
    return (first.unwrap() as? SubtypingRepresentatives)?.sameTypeConstructor(second) ?: false
            || (second.unwrap() as? SubtypingRepresentatives)?.sameTypeConstructor(first) ?: false
}

