/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.DescriptorRendererOptions
import org.jetbrains.kotlin.types.checker.KotlinTypeRefiner
import org.jetbrains.kotlin.types.refinement.TypeRefinement

interface TypeWithEnhancement {
    val origin: UnwrappedType
    val enhancement: KotlinType
}

class SimpleTypeWithEnhancement(
        override val delegate: SimpleType,
        override val enhancement: KotlinType
) : DelegatingSimpleType(),
    TypeWithEnhancement {

    override val origin: UnwrappedType get() = delegate

    override fun replaceAnnotations(newAnnotations: Annotations): SimpleType
            = origin.replaceAnnotations(newAnnotations).wrapEnhancement(enhancement) as SimpleType

    override fun makeNullableAsSpecified(newNullability: Boolean): SimpleType
            = origin.makeNullableAsSpecified(newNullability).wrapEnhancement(enhancement.unwrap().makeNullableAsSpecified(newNullability)) as SimpleType

    @TypeRefinement
    override fun replaceDelegate(delegate: SimpleType) = SimpleTypeWithEnhancement(delegate, enhancement)

    @TypeRefinement
    @OptIn(TypeRefinement::class)
    override fun refine(kotlinTypeRefiner: KotlinTypeRefiner): SimpleTypeWithEnhancement =
            SimpleTypeWithEnhancement(
                kotlinTypeRefiner.refineType(delegate) as SimpleType,
                kotlinTypeRefiner.refineType(enhancement)
            )
}

class FlexibleTypeWithEnhancement(
        override val origin: FlexibleType,
        override val enhancement: KotlinType
) : FlexibleType(origin.lowerBound, origin.upperBound),
    TypeWithEnhancement {

    override fun replaceAnnotations(newAnnotations: Annotations): UnwrappedType
            = origin.replaceAnnotations(newAnnotations).wrapEnhancement(enhancement)

    override fun makeNullableAsSpecified(newNullability: Boolean): UnwrappedType
            = origin.makeNullableAsSpecified(newNullability).wrapEnhancement(enhancement.unwrap().makeNullableAsSpecified(newNullability))

    override fun render(renderer: DescriptorRenderer, options: DescriptorRendererOptions): String {
        if (options.enhancedTypes) {
            return renderer.renderType(enhancement)
        }
        return origin.render(renderer, options)
    }

    override val delegate: SimpleType get() = origin.delegate

    @TypeRefinement
    @OptIn(TypeRefinement::class)
    override fun refine(kotlinTypeRefiner: KotlinTypeRefiner) =
        FlexibleTypeWithEnhancement(
            kotlinTypeRefiner.refineType(origin) as FlexibleType,
            kotlinTypeRefiner.refineType(enhancement)
        )
}

fun KotlinType.getEnhancement(): KotlinType? = when (this) {
    is TypeWithEnhancement -> enhancement
    else -> null
}

private fun List<TypeProjection>.enhanceTypeArguments(depth: Int) =
    map { argument ->
        // TODO: think about star projections with enhancement (e.g. came from Java: Foo<@NotNull ?>)
        if (argument.isStarProjection) {
            return@map argument
        }
        val argumentType = argument.type
        val enhancedArgumentType = if (argumentType is TypeWithEnhancement) argumentType.enhancement else argumentType
        val enhancedDeeplyArgumentType = enhancedArgumentType.getEnhancementDeeply(depth + 1)

        argument.replaceType(enhancedDeeplyArgumentType)
    }

private fun KotlinType.getEnhancementDeeply(depth: Int): KotlinType {
    val newArguments = arguments.enhanceTypeArguments(depth)
    val newArgumentsForUpperBound = if (this is FlexibleType) upperBound.arguments.enhanceTypeArguments(depth) else newArguments
    val enhancedType = if (this is TypeWithEnhancement) enhancement else this

    return enhancedType.replace(
        newArguments = newArguments,
        newArgumentsForUpperBound = newArgumentsForUpperBound
    )
}

fun KotlinType.getEnhancementDeeply(): KotlinType? {
    val enhancedTypeWithArguments = getEnhancementDeeply(depth = 0)

    if (enhancedTypeWithArguments === this) return null

    return enhancedTypeWithArguments
}

fun KotlinType.unwrapEnhancementDeeply() = getEnhancementDeeply() ?: this

fun KotlinType.unwrapEnhancement(): KotlinType = getEnhancement() ?: this

fun UnwrappedType.inheritEnhancement(origin: KotlinType): UnwrappedType = wrapEnhancement(origin.getEnhancement())

fun UnwrappedType.wrapEnhancement(enhancement: KotlinType?): UnwrappedType {
    if (enhancement == null) {
        return this
    }

    return when (this) {
        is SimpleType -> SimpleTypeWithEnhancement(this, enhancement)
        is FlexibleType -> FlexibleTypeWithEnhancement(this, enhancement)
    }
}
