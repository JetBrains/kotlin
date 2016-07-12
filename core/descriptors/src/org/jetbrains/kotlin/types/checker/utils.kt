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

package org.jetbrains.kotlin.types.checker

import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.calls.inference.wrapWithCapturingSubstitution
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeConstructorSubstitution
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.typesApproximation.approximateCapturedTypes
import java.util.*

private class SubtypePathNode(val type: KotlinType, val previous: SubtypePathNode?)

fun findCorrespondingSupertype(
        subtype: KotlinType, supertype: KotlinType,
        typeCheckingProcedureCallbacks: TypeCheckingProcedureCallbacks
): KotlinType? {
    val queue = ArrayDeque<SubtypePathNode>()
    queue.add(SubtypePathNode(subtype, null))

    val supertypeConstructor = supertype.constructor

    while (!queue.isEmpty()) {
        val lastPathNode = queue.poll()
        val currentSubtype = lastPathNode.type
        val constructor = currentSubtype.constructor

        if (typeCheckingProcedureCallbacks.assertEqualTypeConstructors(constructor, supertypeConstructor)) {
            var substituted = currentSubtype
            var isAnyMarkedNullable = currentSubtype.isMarkedNullable

            var currentPathNode = lastPathNode.previous

            while (currentPathNode != null) {
                val currentType = currentPathNode.type
                if (currentType.arguments.any { it.projectionKind != Variance.INVARIANT }) {
                    substituted = TypeConstructorSubstitution.create(currentType)
                                        .wrapWithCapturingSubstitution().buildSubstitutor()
                                        .safeSubstitute(substituted, Variance.INVARIANT)
                                        .approximate()
                }
                else {
                    substituted = TypeConstructorSubstitution.create(currentType)
                                        .buildSubstitutor()
                                        .safeSubstitute(substituted, Variance.INVARIANT)
                }

                isAnyMarkedNullable = isAnyMarkedNullable || currentType.isMarkedNullable

                currentPathNode = currentPathNode.previous
            }

            if (!typeCheckingProcedureCallbacks.assertEqualTypeConstructors(substituted.constructor, supertypeConstructor)) {
                throw AssertionError("Type constructors should be equals!" +
                                     "substitutedSuperType: ${DescriptorRenderer.FQ_NAMES_IN_TYPES.renderType(substituted)}, " +
                                     "foundSupertype: ${DescriptorRenderer.FQ_NAMES_IN_TYPES.renderType(currentSubtype)}, " +
                                     "supertype: ${DescriptorRenderer.FQ_NAMES_IN_TYPES.renderType(supertype)}")
            }

            return TypeUtils.makeNullableAsSpecified(substituted, isAnyMarkedNullable)
        }

        for (immediateSupertype in constructor.supertypes) {
            queue.add(SubtypePathNode(immediateSupertype, lastPathNode))
        }
    }

    return null
}

private fun KotlinType.approximate() = approximateCapturedTypes(this).upper
