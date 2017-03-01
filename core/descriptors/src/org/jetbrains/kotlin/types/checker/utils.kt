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

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.calls.inference.wrapWithCapturingSubstitution
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typesApproximation.approximateCapturedTypes
import java.util.*

private class SubtypePathNode(val type: KotlinType, val previous: SubtypePathNode?)

fun findCorrespondingSupertype(
        subtype: KotlinType, supertype: KotlinType,
        typeCheckingProcedureCallbacks: TypeCheckingProcedureCallbacks = TypeCheckerProcedureCallbacksImpl()
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

            val substitutedConstructor = substituted.constructor
            if (!typeCheckingProcedureCallbacks.assertEqualTypeConstructors(substitutedConstructor, supertypeConstructor)) {
                throw AssertionError("Type constructors should be equals!\n" +
                                     "substitutedSuperType: ${substitutedConstructor.debugInfo()}, \n\n" +
                                     "supertype: ${supertypeConstructor.debugInfo()} \n" +
                                     typeCheckingProcedureCallbacks.assertEqualTypeConstructors(substitutedConstructor, supertypeConstructor))
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

private fun TypeConstructor.debugInfo() = buildString {
    operator fun String.unaryPlus() = appendln(this)

    + "type: ${this@debugInfo}"
    + "hashCode: ${this@debugInfo.hashCode()}"
    + "javaClass: ${this@debugInfo.javaClass.canonicalName}"
    var declarationDescriptor: DeclarationDescriptor? = declarationDescriptor
    while (declarationDescriptor != null) {

        + "fqName: ${DescriptorRenderer.FQ_NAMES_IN_TYPES.render(declarationDescriptor)}"
        + "javaClass: ${declarationDescriptor.javaClass.canonicalName}"

        declarationDescriptor = declarationDescriptor.containingDeclaration
    }
}
