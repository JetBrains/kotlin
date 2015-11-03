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

package org.jetbrains.kotlin.resolve.calls.inference

import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPositionKind
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.derivedFrom
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeProjection
import org.jetbrains.kotlin.types.TypeProjectionImpl
import java.util.*

fun ConstraintSystem.getNestedTypeVariables(type: KotlinType): List<TypeParameterDescriptor> =
        type.getNestedTypeParameters().filter { it in typeParameterDescriptors }.map { descriptorToVariable(it) }

fun ConstraintSystem.filterConstraintsOut(excludePositionKind: ConstraintPositionKind): ConstraintSystem {
    return toBuilder { !it.derivedFrom(excludePositionKind) }.build()
}

internal fun KotlinType.getNestedArguments(): List<TypeProjection> {
    val result = ArrayList<TypeProjection>()

    val stack = ArrayDeque<TypeProjection>()
    stack.push(TypeProjectionImpl(this))

    while (!stack.isEmpty()) {
        val typeProjection = stack.pop()
        if (typeProjection.isStarProjection) continue

        result.add(typeProjection)

        typeProjection.type.arguments.forEach { stack.add(it) }
    }
    return result
}

internal fun KotlinType.getNestedTypeParameters(): List<TypeParameterDescriptor> {
    return getNestedArguments().map { typeProjection ->
        typeProjection.type.constructor.declarationDescriptor as? TypeParameterDescriptor
    }.filterNotNull()
}
