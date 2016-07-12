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

package org.jetbrains.kotlin.load.kotlin

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationsImpl
import org.jetbrains.kotlin.descriptors.annotations.composeAnnotations
import org.jetbrains.kotlin.descriptors.annotations.createUnsafeVarianceAnnotation
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.replaceAnnotations

internal class UnsafeVarianceTypeSubstitution(kotlinBuiltIns: KotlinBuiltIns) : TypeSubstitution() {
    private val unsafeVarianceAnnotations = AnnotationsImpl(listOf(kotlinBuiltIns.createUnsafeVarianceAnnotation()))

    override fun get(key: KotlinType) = null

    override fun prepareTopLevelType(topLevelType: KotlinType, position: Variance): KotlinType {
        val unsafeVariancePaths = mutableListOf<List<Int>>()
        IndexedTypeHolder(topLevelType).checkTypePosition(
                position,
                { typeParameter, indexedTypeHolder, errorPosition ->
                    unsafeVariancePaths.add(indexedTypeHolder.argumentIndices)
                },
                customVariance = { null })

        return topLevelType.annotatePartsWithUnsafeVariance(unsafeVariancePaths)
    }

    private fun KotlinType.annotatePartsWithUnsafeVariance(unsafeVariancePaths: Collection<List<Int>>): KotlinType {
        if (unsafeVariancePaths.isEmpty()) return this

        if (isFlexible()) {
            return flexibility().factory.create(
                    lowerIfFlexible().annotatePartsWithUnsafeVariance(subPathsWithIndex(unsafeVariancePaths, 0)),
                    upperIfFlexible().annotatePartsWithUnsafeVariance(subPathsWithIndex(unsafeVariancePaths, 1))
            )
        }

        // if root is unsafe
        if (emptyList<Int>() in unsafeVariancePaths) {
            return replaceAnnotations(composeAnnotations(annotations, unsafeVarianceAnnotations))
        }

        return replace(newArguments = arguments.withIndex().map {
                    val (index, argument) = it
                    if (argument.isStarProjection) return@map argument
            TypeProjectionImpl(
                    argument.projectionKind,
                    argument.type.annotatePartsWithUnsafeVariance(subPathsWithIndex(unsafeVariancePaths, index)))
                })
    }

    private fun subPathsWithIndex(paths: Collection<List<Int>>, index: Int) = paths.filter { it[0] == index }.map { it.subList(1, it.size) }

    private class IndexedTypeHolder(
            override val type: KotlinType,
            val argumentIndices: List<Int> = emptyList()
    ) : TypeHolder<IndexedTypeHolder> {
        override val flexibleBounds: Pair<IndexedTypeHolder, IndexedTypeHolder>? get() =
            if (type.isFlexible())
                Pair(
                        IndexedTypeHolder(type.lowerIfFlexible(), argumentIndices + 0),
                        IndexedTypeHolder(type.upperIfFlexible(), argumentIndices + 1))
            else null

        override val arguments: List<TypeHolderArgument<IndexedTypeHolder>>
            get() = type.arguments.withIndex().map { projectionWithIndex ->

                val (index, projection) = projectionWithIndex
                object : TypeHolderArgument<IndexedTypeHolder> {
                    override val projection: TypeProjection
                        get() = projection
                    override val typeParameter: TypeParameterDescriptor?
                        get() = type.constructor.parameters[index]
                    override val holder: IndexedTypeHolder
                        get() = IndexedTypeHolder(projection.type, argumentIndices + index)

                }
            }

    }
}
