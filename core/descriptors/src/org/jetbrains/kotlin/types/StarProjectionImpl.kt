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

package org.jetbrains.kotlin.types

import kotlin.properties.*
import org.jetbrains.kotlin.descriptors.*

class StarProjectionImpl(
        private val typeParameter: TypeParameterDescriptor
) : TypeProjectionBase() {
    override fun isStarProjection() = true

    override fun getProjectionKind() = Variance.OUT_VARIANCE

    // No synchronization here: there's no problem in accidentally computing this twice
    private val _type: JetType by Delegates.lazy {
        typeParameter.starProjectionType()
    }

    override fun getType() = _type
}

public fun TypeParameterDescriptor.starProjectionType(): JetType {
    val classDescriptor = this.getContainingDeclaration() as ClassDescriptor
    val typeParameters = classDescriptor.getTypeConstructor().getParameters().map { it.getTypeConstructor() }
    return TypeSubstitutor.create(
            object : TypeSubstitution() {
                override fun get(key: TypeConstructor) =
                        if (key in typeParameters)
                            TypeUtils.makeStarProjection(key.getDeclarationDescriptor() as TypeParameterDescriptor)
                        else null

            }
    ).substitute(this.getUpperBounds().first(), Variance.OUT_VARIANCE)!!
}

class TypeBasedStarProjectionImpl(
        private val _type: JetType
) : TypeProjectionBase() {
    override fun isStarProjection() = true

    override fun getProjectionKind() = Variance.OUT_VARIANCE

    override fun getType() = _type
}