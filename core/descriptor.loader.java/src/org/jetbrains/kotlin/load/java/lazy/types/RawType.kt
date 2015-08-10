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

package org.jetbrains.kotlin.load.java.lazy.types

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.load.java.components.TypeUsage
import org.jetbrains.kotlin.renderer.CustomFlexibleRendering
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.types.*

public object RawTypeCapabilities : TypeCapabilities {
    private object RawSubstitutionCapability : CustomSubstitutionCapability {
        override val substitution = RawSubstitution
    }

    private object RawFlexibleRendering : CustomFlexibleRendering {
        private fun DescriptorRenderer.renderArguments(jetType: JetType) = jetType.arguments.map { renderTypeProjection(it) }

        private fun String.replaceArgs(newArgs: String): String {
            if (!contains('<')) return this
            return "${substringBefore('<')}<$newArgs>${substringAfterLast('>')}"
        }

        override fun renderInflexible(type: JetType, renderer: DescriptorRenderer): String? {
            if (type.arguments.isNotEmpty()) return null

            return StringBuilder {
                append(renderer.renderTypeConstructor(type.constructor))
                append("(raw)")
                if (type.isMarkedNullable) append('?')
            }.toString()
        }

        override fun renderBounds(flexibility: Flexibility, renderer: DescriptorRenderer): Pair<String, String>? {
            val lowerArgs = renderer.renderArguments(flexibility.lowerBound)
            val upperArgs = renderer.renderArguments(flexibility.upperBound)

            val lowerRendered = renderer.renderType(flexibility.lowerBound)
            val upperRendered = renderer.renderType(flexibility.upperBound)

            if (!upperArgs.isNotEmpty()) return null

            val newArgs = lowerArgs.map { "(raw) $it" }.join(", ")
            val newUpper =
                    if (lowerArgs.zip(upperArgs).all { onlyOutDiffers(it.first, it.second) })
                        upperRendered.replaceArgs(newArgs)
                    else upperRendered
            return Pair(lowerRendered.replaceArgs(newArgs), newUpper)
        }

        private fun onlyOutDiffers(first: String, second: String) = first == second.removePrefix("out ") || second == "*"
    }

    override fun <T : TypeCapability> getCapability(capabilityClass: Class<T>): T? {
        @suppress("UNCHECKED_CAST")
        return when(capabilityClass) {
            javaClass<CustomSubstitutionCapability>() -> RawSubstitutionCapability as T
            javaClass<CustomFlexibleRendering>() -> RawFlexibleRendering as T
            else -> null
        }
    }
}

private object RawSubstitution : TypeSubstitution() {
    override fun get(key: JetType) = TypeProjectionImpl(eraseType(key))

    private val lowerTypeAttr = TypeUsage.MEMBER_SIGNATURE_INVARIANT.toAttributes().toFlexible(JavaTypeFlexibility.FLEXIBLE_LOWER_BOUND)
    private val upperTypeAttr = TypeUsage.MEMBER_SIGNATURE_INVARIANT.toAttributes().toFlexible(JavaTypeFlexibility.FLEXIBLE_UPPER_BOUND)

    private fun eraseType(type: JetType): JetType {
        val declaration = type.constructor.declarationDescriptor
        return when (declaration) {
            is TypeParameterDescriptor -> eraseType(declaration.getErasedUpperBound())
            is ClassDescriptor -> {
                val lower = type.lowerIfFlexible()
                val upper = type.upperIfFlexible()
                LazyJavaTypeResolver.FlexibleJavaClassifierTypeCapabilities.create(
                        eraseInflexibleBasedOnClassDescriptor(lower, declaration, lowerTypeAttr),
                        eraseInflexibleBasedOnClassDescriptor(upper, declaration, upperTypeAttr)
                )
            }
            else -> error("Unexpected declaration kind: $declaration")
        }
    }

    private fun eraseInflexibleBasedOnClassDescriptor(type: JetType, declaration: ClassDescriptor, attr: JavaTypeAttributes): JetType {
        if (KotlinBuiltIns.isArray(type)) {
            val componentTypeProjection = type.arguments[0]
            val arguments = listOf(
                    TypeProjectionImpl(componentTypeProjection.projectionKind, eraseType(componentTypeProjection.type))
            )
            return JetTypeImpl.create(
                    type.annotations, type.constructor, type.isMarkedNullable, arguments,
                    (type.constructor.declarationDescriptor as ClassDescriptor).getMemberScope(arguments)
            )
        }

        val constructor = type.constructor
        return JetTypeImpl.create(
                type.annotations, constructor, type.isMarkedNullable,
                type.constructor.parameters.map {
                    parameter ->
                    computeProjection(parameter, attr)
                },
                RawSubstitution,
                declaration.getMemberScope(RawSubstitution),
                RawTypeCapabilities
        )
    }

    fun computeProjection(
            parameter: TypeParameterDescriptor,
            attr: JavaTypeAttributes,
            erasedUpperBound: JetType = parameter.getErasedUpperBound()
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
                TypeProjectionImpl(Variance.INVARIANT, parameter.lowerBounds.first())
            else if (erasedUpperBound.constructor.parameters.isNotEmpty())
                // T : Enum<E> -> out Enum<*>
                TypeProjectionImpl(Variance.OUT_VARIANCE, erasedUpperBound)
            else
                // T : String -> *
                makeStarProjection(parameter, attr)
        }
    }

    override fun isEmpty() = false
}
