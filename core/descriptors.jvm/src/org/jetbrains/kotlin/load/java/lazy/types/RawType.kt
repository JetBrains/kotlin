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

package org.jetbrains.kotlin.load.java.lazy.types

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.load.java.components.TypeUsage
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.DescriptorRendererOptions
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.checker.KotlinTypeRefiner
import org.jetbrains.kotlin.types.TypeRefinement
import org.jetbrains.kotlin.types.typeUtil.builtIns

class RawTypeImpl private constructor(lowerBound: SimpleType, upperBound: SimpleType, disableAssertion: Boolean) :
    FlexibleType(lowerBound, upperBound), RawType {

    constructor(lowerBound: SimpleType, upperBound: SimpleType) : this(lowerBound, upperBound, false)

    init {
        if (!disableAssertion) {
            assert(KotlinTypeChecker.DEFAULT.isSubtypeOf(lowerBound, upperBound)) {
                "Lower bound $lowerBound of a flexible type must be a subtype of the upper bound $upperBound"
            }
        }
    }

    override val delegate: SimpleType get() = lowerBound

    override val memberScope: MemberScope
        get() {
            val classDescriptor = constructor.declarationDescriptor as? ClassDescriptor
                ?: error("Incorrect classifier: ${constructor.declarationDescriptor}")
            return classDescriptor.getMemberScope(RawSubstitution())
        }

    override fun replaceAnnotations(newAnnotations: Annotations) =
        RawTypeImpl(lowerBound.replaceAnnotations(newAnnotations), upperBound.replaceAnnotations(newAnnotations))

    override fun makeNullableAsSpecified(newNullability: Boolean) =
        RawTypeImpl(lowerBound.makeNullableAsSpecified(newNullability), upperBound.makeNullableAsSpecified(newNullability))

    override fun render(renderer: DescriptorRenderer, options: DescriptorRendererOptions): String {
        fun onlyOutDiffers(first: String, second: String) = first == second.removePrefix("out ") || second == "*"

        fun renderArguments(type: KotlinType) = type.arguments.map { renderer.renderTypeProjection(it) }

        fun String.replaceArgs(newArgs: String): String {
            if (!contains('<')) return this
            return "${substringBefore('<')}<$newArgs>${substringAfterLast('>')}"
        }

        val lowerRendered = renderer.renderType(lowerBound)
        val upperRendered = renderer.renderType(upperBound)

        if (options.debugMode) {
            return "raw ($lowerRendered..$upperRendered)"
        }
        if (upperBound.arguments.isEmpty()) return renderer.renderFlexibleType(lowerRendered, upperRendered, builtIns)

        val lowerArgs = renderArguments(lowerBound)
        val upperArgs = renderArguments(upperBound)
        val newArgs = lowerArgs.joinToString(", ") { "(raw) $it" }
        val newUpper =
            if (lowerArgs.zip(upperArgs).all { onlyOutDiffers(it.first, it.second) })
                upperRendered.replaceArgs(newArgs)
            else upperRendered
        val newLower = lowerRendered.replaceArgs(newArgs)
        if (newLower == newUpper) return newLower
        return renderer.renderFlexibleType(newLower, newUpper, builtIns)
    }

    @TypeRefinement
    @OptIn(TypeRefinement::class)
    override fun refine(kotlinTypeRefiner: KotlinTypeRefiner): FlexibleType {
        return RawTypeImpl(
            kotlinTypeRefiner.refineType(lowerBound) as SimpleType,
            kotlinTypeRefiner.refineType(upperBound) as SimpleType,
            disableAssertion = true
        )
    }
}

internal class RawSubstitution(typeParameterUpperBoundEraser: TypeParameterUpperBoundEraser? = null) : TypeSubstitution() {
    private val typeParameterUpperBoundEraser = typeParameterUpperBoundEraser ?: TypeParameterUpperBoundEraser(this)

    override fun get(key: KotlinType) = TypeProjectionImpl(eraseType(key))

    private fun eraseType(type: KotlinType, attr: JavaTypeAttributes = JavaTypeAttributes(TypeUsage.COMMON)): KotlinType {
        return when (val declaration = type.constructor.declarationDescriptor) {
            is TypeParameterDescriptor ->
                eraseType(typeParameterUpperBoundEraser.getErasedUpperBound(declaration, isRaw = true, attr), attr)
            is ClassDescriptor -> {
                val declarationForUpper =
                    type.upperIfFlexible().constructor.declarationDescriptor

                check(declarationForUpper is ClassDescriptor) {
                    "For some reason declaration for upper bound is not a class " +
                            "but \"$declarationForUpper\" while for lower it's \"$declaration\""
                }

                val (lower, isRawL) = eraseInflexibleBasedOnClassDescriptor(type.lowerIfFlexible(), declaration, lowerTypeAttr)
                val (upper, isRawU) = eraseInflexibleBasedOnClassDescriptor(type.upperIfFlexible(), declarationForUpper, upperTypeAttr)

                if (isRawL || isRawU) {
                    RawTypeImpl(lower, upper)
                } else {
                    KotlinTypeFactory.flexibleType(lower, upper)
                }
            }
            else -> error("Unexpected declaration kind: $declaration")
        }
    }

    // false means that type cannot be raw
    private fun eraseInflexibleBasedOnClassDescriptor(
        type: SimpleType, declaration: ClassDescriptor, attr: JavaTypeAttributes
    ): Pair<SimpleType, Boolean> {
        if (type.constructor.parameters.isEmpty()) return type to false

        if (KotlinBuiltIns.isArray(type)) {
            val componentTypeProjection = type.arguments[0]
            val arguments = listOf(
                TypeProjectionImpl(componentTypeProjection.projectionKind, eraseType(componentTypeProjection.type, attr))
            )
            return KotlinTypeFactory.simpleType(
                type.annotations, type.constructor, arguments, type.isMarkedNullable
            ) to false
        }

        if (type.isError) return ErrorUtils.createErrorType("Raw error type: ${type.constructor}") to false

        val memberScope = declaration.getMemberScope(this)
        return KotlinTypeFactory.simpleTypeWithNonTrivialMemberScope(
            type.annotations, declaration.typeConstructor,
            declaration.typeConstructor.parameters.map { parameter ->
                computeProjection(parameter, attr)
            },
            type.isMarkedNullable, memberScope
        ) factory@{ kotlinTypeRefiner ->
            val classId = (declaration as? ClassDescriptor)?.classId ?: return@factory null
            @OptIn(TypeRefinement::class)
            val refinedClassDescriptor = kotlinTypeRefiner.findClassAcrossModuleDependencies(classId) ?: return@factory null
            if (refinedClassDescriptor == declaration) return@factory null

            eraseInflexibleBasedOnClassDescriptor(type, refinedClassDescriptor, attr).first
        } to true
    }

    fun computeProjection(
        parameter: TypeParameterDescriptor,
        attr: JavaTypeAttributes,
        erasedUpperBound: KotlinType = typeParameterUpperBoundEraser.getErasedUpperBound(parameter, isRaw = true, attr)
    ) = when (attr.flexibility) {
        // Raw(List<T>) => (List<Any?>..List<*>)
        // Raw(Enum<T>) => (Enum<Enum<*>>..Enum<out Enum<*>>)
        // In the last case upper bound is equal to star projection `Enum<*>`,
        // but we want to keep matching tree structure of flexible bounds (at least they should have the same size)
        JavaTypeFlexibility.FLEXIBLE_LOWER_BOUND -> TypeProjectionImpl(
            // T : String -> String
            // in T : String -> String
            // T : Enum<T> -> Enum<*>
            Variance.INVARIANT, erasedUpperBound
        )
        JavaTypeFlexibility.FLEXIBLE_UPPER_BOUND, JavaTypeFlexibility.INFLEXIBLE -> {
            if (!parameter.variance.allowsOutPosition)
            // in T -> Comparable<Nothing>
                TypeProjectionImpl(Variance.INVARIANT, parameter.builtIns.nothingType)
            else if (erasedUpperBound.constructor.parameters.isNotEmpty())
            // T : Enum<E> -> out Enum<*>
                TypeProjectionImpl(Variance.OUT_VARIANCE, erasedUpperBound)
            else
            // T : String -> *
                makeStarProjection(parameter, attr)
        }
    }

    override fun isEmpty() = false

    companion object {
        private val lowerTypeAttr = TypeUsage.COMMON.toAttributes().withFlexibility(JavaTypeFlexibility.FLEXIBLE_LOWER_BOUND)
        private val upperTypeAttr = TypeUsage.COMMON.toAttributes().withFlexibility(JavaTypeFlexibility.FLEXIBLE_UPPER_BOUND)
    }
}
