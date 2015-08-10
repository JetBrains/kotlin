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

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.Variance.IN_VARIANCE
import org.jetbrains.kotlin.types.Variance.OUT_VARIANCE

public class CapturedTypeConstructor(
        public val typeProjection: TypeProjection
): TypeConstructor {
    init {
        assert(typeProjection.getProjectionKind() != Variance.INVARIANT) {
            "Only nontrivial projections can be captured, not: $typeProjection"
        }
    }

    override fun getParameters(): List<TypeParameterDescriptor> = listOf()

    override fun getSupertypes(): Collection<JetType> {
        val superType = if (typeProjection.getProjectionKind() == Variance.OUT_VARIANCE)
            typeProjection.getType()
        else
            KotlinBuiltIns.getInstance().getNullableAnyType()
        return listOf(superType)
    }

    override fun isFinal() = true

    override fun isDenotable() = false

    override fun getDeclarationDescriptor() = null

    override fun getAnnotations() = Annotations.EMPTY

    override fun toString() = "CapturedTypeConstructor($typeProjection)"
}

public class CapturedType(
        private val typeProjection: TypeProjection
): DelegatingType(), SubtypingRepresentatives {

    private val delegateType = run {
        val scope = ErrorUtils.createErrorScope(
                "No member resolution should be done on captured type, it used only during constraint system resolution", true)
        JetTypeImpl.create(Annotations.EMPTY, CapturedTypeConstructor(typeProjection), false, listOf(), scope)
    }

    override fun getDelegate(): JetType = delegateType

    override fun <T : TypeCapability> getCapability(capabilityClass: Class<T>): T? {
        @suppress("UNCHECKED_CAST")
        return if (capabilityClass == javaClass<SubtypingRepresentatives>()) this as T
        else super<DelegatingType>.getCapability(capabilityClass)
    }

    override val subTypeRepresentative: JetType
        get() = representative(OUT_VARIANCE, KotlinBuiltIns.getInstance().getNullableAnyType())

    override val superTypeRepresentative: JetType
        get() = representative(IN_VARIANCE, KotlinBuiltIns.getInstance().getNothingType())

    private fun representative(variance: Variance, default: JetType) =
        if (typeProjection.getProjectionKind() == variance) typeProjection.getType() else default

    override fun sameTypeConstructor(type: JetType) = delegateType.getConstructor() === type.getConstructor()

    override fun toString() = "Captured($typeProjection)"
}

public fun createCapturedType(typeProjection: TypeProjection): JetType = CapturedType(typeProjection)

public fun JetType.isCaptured(): Boolean = getConstructor() is CapturedTypeConstructor
