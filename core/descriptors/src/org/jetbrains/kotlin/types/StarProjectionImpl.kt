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

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassifierDescriptorWithTypeParameters
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.types.checker.KotlinTypeRefiner

class StarProjectionImpl(
    private val typeParameter: TypeParameterDescriptor
) : TypeProjectionBase() {
    override fun isStarProjection() = true

    override fun getProjectionKind() = Variance.OUT_VARIANCE

    // No synchronization here: there's no problem in accidentally computing this twice
    private val _type: KotlinType by lazy(LazyThreadSafetyMode.PUBLICATION) {
        typeParameter.starProjectionType()
    }

    override fun getType() = _type

    @TypeRefinement
    override fun refine(kotlinTypeRefiner: KotlinTypeRefiner): TypeProjection = this

    override fun replaceType(type: KotlinType): TypeProjection {
        throw UnsupportedOperationException("Replacing type for star projection is unsupported")
    }
}

private fun buildStarProjectionTypeByTypeParameters(
    typeParameters: List<TypeConstructor>,
    upperBounds: List<KotlinType>,
    builtIns: KotlinBuiltIns
) = TypeSubstitutor.create(
    object : TypeConstructorSubstitution() {
        override fun get(key: TypeConstructor) =
            if (key in typeParameters)
                TypeUtils.makeStarProjection(key.declarationDescriptor as TypeParameterDescriptor)
            else null

    }
).substitute(upperBounds.first(), Variance.OUT_VARIANCE) ?: builtIns.defaultBound

fun TypeParameterDescriptor.starProjectionType(): KotlinType {
    return when (val descriptor = this.containingDeclaration) {
        is ClassifierDescriptorWithTypeParameters -> {
            buildStarProjectionTypeByTypeParameters(
                typeParameters = descriptor.typeConstructor.parameters.map { it.typeConstructor },
                upperBounds,
                builtIns
            )
        }
        is FunctionDescriptor -> {
            buildStarProjectionTypeByTypeParameters(
                typeParameters = descriptor.typeParameters.map { it.typeConstructor },
                upperBounds,
                builtIns
            )
        }
        else -> throw IllegalArgumentException("Unsupported descriptor type to build star projection type based on type parameters of it")
    }
}

// It should only be used in rare cases when type parameter for the relevant argument is not available
class StarProjectionForAbsentTypeParameter(
    kotlinBuiltIns: KotlinBuiltIns
) : TypeProjectionBase() {
    private val nullableAnyType: KotlinType = kotlinBuiltIns.nullableAnyType

    override fun isStarProjection() = true

    override fun getProjectionKind() = Variance.OUT_VARIANCE

    override fun getType() = nullableAnyType

    @TypeRefinement
    override fun refine(kotlinTypeRefiner: KotlinTypeRefiner): TypeProjection = this

    override fun replaceType(type: KotlinType): TypeProjection {
        throw UnsupportedOperationException("Replacing type for star projection is unsupported")
    }
}
