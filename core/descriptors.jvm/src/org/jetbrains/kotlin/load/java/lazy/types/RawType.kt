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

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.DescriptorRendererOptions
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

    override fun replaceAttributes(newAttributes: TypeAttributes) =
        RawTypeImpl(lowerBound.replaceAttributes(newAttributes), upperBound.replaceAttributes(newAttributes))


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
