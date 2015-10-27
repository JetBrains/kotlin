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

package org.jetbrains.kotlin.resolve.calls.inference

import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.resolve.calls.inference.TypeBounds.BoundKind
import org.jetbrains.kotlin.resolve.calls.inference.TypeBounds.BoundKind.EXACT_BOUND
import org.jetbrains.kotlin.resolve.calls.inference.TypeBounds.BoundKind.LOWER_BOUND
import org.jetbrains.kotlin.resolve.calls.inference.TypeBounds.BoundKind.UPPER_BOUND
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPosition
import org.jetbrains.kotlin.types.KotlinType

public interface TypeBounds {
    public val typeVariable: TypeParameterDescriptor

    public val bounds: Collection<Bound>

    public val value: KotlinType?
        get() = if (values.size == 1) values.first() else null

    public val values: Collection<KotlinType>

    public enum class BoundKind {
        LOWER_BOUND,
        EXACT_BOUND,
        UPPER_BOUND
    }

    public class Bound(
            public val typeVariable: TypeParameterDescriptor,
            public val constrainingType: KotlinType,
            public val kind: BoundKind,
            public val position: ConstraintPosition,
            public val isProper: Boolean,
            // to prevent infinite recursion in incorporation we store the variables that was substituted to derive this bound
            public val derivedFrom: Set<TypeParameterDescriptor>
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || javaClass != other.javaClass) return false

            val bound = other as Bound

            if (typeVariable != bound.typeVariable) return false
            if (constrainingType != bound.constrainingType) return false
            if (kind != bound.kind) return false

            if (position.isStrong() != bound.position.isStrong()) return false

            return true
        }

        override fun hashCode(): Int {
            var result = typeVariable.hashCode();
            result = 31 * result + constrainingType.hashCode()
            result = 31 * result + kind.hashCode()
            result = 31 * result + if (position.isStrong()) 1 else 0
            return result
        }

        override fun toString() = "Bound($constrainingType, $kind, $position, isProper = $isProper)"
    }
}

fun BoundKind.reverse() = when (this) {
    LOWER_BOUND -> UPPER_BOUND
    UPPER_BOUND -> LOWER_BOUND
    EXACT_BOUND -> EXACT_BOUND
}
