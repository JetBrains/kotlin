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

import org.jetbrains.kotlin.descriptors.ClassifierDescriptorWithTypeParameters
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.module

class StarProjectionImpl(
    private val typeParameter: TypeParameterDescriptor,
    private val moduleDescriptor: ModuleDescriptor
) : TypeProjectionBase() {

    constructor(typeParameter: TypeParameterDescriptor) : this(typeParameter, typeParameter.module)

    override fun isStarProjection() = true

    override fun getProjectionKind() = Variance.OUT_VARIANCE

    // No synchronization here: there's no problem in accidentally computing this twice
    private val _type: KotlinType by lazy(LazyThreadSafetyMode.PUBLICATION) {
        typeParameter.starProjectionType().refine(moduleDescriptor)
    }

    override fun getType() = _type

    override fun refine(moduleDescriptor: ModuleDescriptor) = StarProjectionImpl(typeParameter, moduleDescriptor)
}

fun TypeParameterDescriptor.starProjectionType(): KotlinType {
    val classDescriptor = this.containingDeclaration as ClassifierDescriptorWithTypeParameters
    val typeParameters = classDescriptor.typeConstructor.parameters.map { it.typeConstructor }
    return TypeSubstitutor.create(
            object : TypeConstructorSubstitution() {
                override fun get(key: TypeConstructor) =
                        if (key in typeParameters)
                            TypeUtils.makeStarProjection(key.declarationDescriptor as TypeParameterDescriptor)
                        else null

            }
    ).substitute(this.upperBounds.first(), Variance.OUT_VARIANCE) ?: builtIns.defaultBound
}

class TypeBasedStarProjectionImpl(
        private val _type: KotlinType
) : TypeProjectionBase() {
    override fun isStarProjection() = true

    override fun getProjectionKind() = Variance.OUT_VARIANCE

    override fun getType() = _type

    override fun refine(moduleDescriptor: ModuleDescriptor): TypeProjection = TypeBasedStarProjectionImpl(_type.refine(moduleDescriptor!!))
}
