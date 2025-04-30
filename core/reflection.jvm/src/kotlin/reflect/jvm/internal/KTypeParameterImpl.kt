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

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.runtime.components.ReflectKotlinClass
import org.jetbrains.kotlin.load.kotlin.JvmPackagePartSource
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedMemberDescriptor
import org.jetbrains.kotlin.types.Variance
import kotlin.jvm.internal.TypeParameterReference
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.KVariance

internal class KTypeParameterImpl(
    container: KTypeParameterOwnerImpl?,
    override val descriptor: TypeParameterDescriptor,
) : KTypeParameter, KClassifierImpl {
    override val name: String
        get() = descriptor.name.asString()

    override val upperBounds: List<KType> by ReflectProperties.lazySoft { descriptor.upperBounds.map(::KTypeImpl) }

    override val variance: KVariance
        get() = when (descriptor.variance) {
            Variance.INVARIANT -> KVariance.INVARIANT
            Variance.IN_VARIANCE -> KVariance.IN
            Variance.OUT_VARIANCE -> KVariance.OUT
        }

    override val isReified: Boolean
        get() = descriptor.isReified

    private val container: KTypeParameterOwnerImpl by ReflectProperties.lazySoft {
        container ?: when (val declaration = descriptor.containingDeclaration) {
            is ClassDescriptor -> {
                declaration.toKClassImpl()
            }
            is CallableMemberDescriptor -> {
                val callableContainerClass = when (val callableContainer = declaration.containingDeclaration) {
                    is ClassDescriptor -> {
                        callableContainer.toKClassImpl()
                    }
                    else -> {
                        val deserializedMember = declaration as? DeserializedMemberDescriptor
                            ?: throw KotlinReflectionInternalError("Non-class callable descriptor must be deserialized: $declaration")
                        deserializedMember.getContainerClass().kotlin as KClassImpl<*>
                    }
                }
                declaration.accept(CreateKCallableVisitor(callableContainerClass), Unit)
            }
            else -> throw KotlinReflectionInternalError("Unknown type parameter container: $declaration")
        }
    }

    private fun ClassDescriptor.toKClassImpl(): KClassImpl<*> =
        toJavaClass()?.kotlin as KClassImpl<*>?
            ?: throw KotlinReflectionInternalError("Type parameter container is not resolved: $containingDeclaration")

    private fun DeserializedMemberDescriptor.getContainerClass(): Class<*> {
        val jvmPackagePartSource = containerSource as? JvmPackagePartSource
        return (jvmPackagePartSource?.knownJvmBinaryClass as? ReflectKotlinClass)?.klass
            ?: throw KotlinReflectionInternalError("Container of deserialized member is not resolved: $this")
    }

    override fun equals(other: Any?) =
        other is KTypeParameterImpl && container == other.container && name == other.name

    override fun hashCode() =
        container.hashCode() * 31 + name.hashCode()

    override fun toString() =
        TypeParameterReference.toString(this)
}
