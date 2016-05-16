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

import org.jetbrains.kotlin.descriptors.annotations.Annotations

class DisjointKeysUnionTypeSubstitution private constructor(
    private val first: TypeSubstitution,
    private val second: TypeSubstitution
) : TypeSubstitution() {
    companion object {
        @JvmStatic fun create(first: TypeSubstitution, second: TypeSubstitution): TypeSubstitution {
            if (first.isEmpty()) return second
            if (second.isEmpty()) return first

            return DisjointKeysUnionTypeSubstitution(first, second)
        }
    }

    override fun get(key: KotlinType) = first[key] ?: second[key]
    override fun prepareTopLevelType(topLevelType: KotlinType, position: Variance) =
            second.prepareTopLevelType(first.prepareTopLevelType(topLevelType, position), position)

    override fun isEmpty() = false

    override fun approximateCapturedTypes() = first.approximateCapturedTypes() || second.approximateCapturedTypes()
    override fun approximateContravariantCapturedTypes() = first.approximateContravariantCapturedTypes() || second.approximateContravariantCapturedTypes()

    override fun filterAnnotations(annotations: Annotations) = second.filterAnnotations(first.filterAnnotations(annotations))
}
