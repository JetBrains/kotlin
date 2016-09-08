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

package kotlin.reflect.jvm.internal

import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.types.Variance
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.KVariance

internal class KTypeParameterImpl(override val descriptor: TypeParameterDescriptor) : KTypeParameter, KClassifierImpl {
    override val name: String
        get() = descriptor.name.asString()

    override val upperBounds: List<KType> by ReflectProperties.lazySoft {
        descriptor.upperBounds.map { kotlinType ->
            KTypeImpl(kotlinType) {
                TODO("Java type is not yet supported for type parameters: $descriptor")
            }
        }
    }

    override val variance: KVariance
        get() = when (descriptor.variance) {
            Variance.INVARIANT -> KVariance.INVARIANT
            Variance.IN_VARIANCE -> KVariance.IN
            Variance.OUT_VARIANCE -> KVariance.OUT
        }

    override val isReified: Boolean
        get() = descriptor.isReified

    override fun equals(other: Any?) =
            other is KTypeParameterImpl && descriptor == other.descriptor

    override fun hashCode() =
            descriptor.hashCode()

    override fun toString() =
            ReflectionObjectRenderer.renderTypeParameter(descriptor)
}
