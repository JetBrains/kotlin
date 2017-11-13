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

package org.jetbrains.kotlin.platform

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.types.TypeConstructorSubstitution
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection

fun createMappedTypeParametersSubstitution(from: ClassDescriptor, to: ClassDescriptor): TypeConstructorSubstitution {
    assert(from.declaredTypeParameters.size == to.declaredTypeParameters.size) {
        "$from and $to should have same number of type parameters, " +
        "but ${from.declaredTypeParameters.size} / ${to.declaredTypeParameters.size} found"
    }

    return TypeConstructorSubstitution.createByConstructorsMap(
            from.declaredTypeParameters.map(TypeParameterDescriptor::getTypeConstructor).zip(
                    to.declaredTypeParameters.map { it.defaultType.asTypeProjection() }
            ).toMap())
}