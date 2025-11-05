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
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.kotlin.types.model.TypeParameterMarker
import kotlin.jvm.internal.KTypeParameterBase
import kotlin.jvm.internal.Reflection
import kotlin.reflect.KType
import kotlin.reflect.KVariance
import kotlin.reflect.jvm.ReflectedLambdaFakeContainerSource
import kotlin.reflect.jvm.internal.types.DescriptorKType

internal class KTypeParameterImpl private constructor(
    descriptor: TypeParameterDescriptor?,
    container: KTypeParameterOwnerImpl,
    override val name: String,
    override val variance: KVariance,
    override val isReified: Boolean,
) : KTypeParameterBase(container), TypeParameterMarker, TypeConstructorMarker {
    constructor(
        container: KTypeParameterOwnerImpl,
        name: String,
        variance: KVariance,
        isReified: Boolean,
    ) : this(descriptor = null, container, name, variance, isReified)

    constructor(container: KTypeParameterOwnerImpl, descriptor: TypeParameterDescriptor) : this(
        descriptor,
        container,
        descriptor.name.asString(),
        descriptor.variance.toKVariance(),
        descriptor.isReified,
    ) {
        upperBounds = descriptor.upperBounds.map(::DescriptorKType)
    }

    private val _descriptor: TypeParameterDescriptor? = descriptor
    val descriptor: TypeParameterDescriptor
        get() = _descriptor ?: error("Descriptor-less type parameter: $this")

    @Volatile
    override lateinit var upperBounds: List<KType>
}

private fun Variance.toKVariance(): KVariance =
    when (this) {
        Variance.INVARIANT -> KVariance.INVARIANT
        Variance.IN_VARIANCE -> KVariance.IN
        Variance.OUT_VARIANCE -> KVariance.OUT
    }

internal fun TypeParameterDescriptor.toContainer(): KTypeParameterOwnerImpl =
    when (val declaration = containingDeclaration) {
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
                    deserializedMember.getContainerOfDeserializedMember()
                }
            }
            declaration.accept(CreateKCallableVisitor(callableContainerClass), Unit)
        }
        else -> throw KotlinReflectionInternalError("Unknown type parameter container: $declaration")
    }

private fun ClassDescriptor.toKClassImpl(): KClassImpl<*> =
    toJavaClass()?.kotlin as KClassImpl<*>?
        ?: throw KotlinReflectionInternalError("Type parameter container is not resolved: $containingDeclaration")

private fun DeserializedMemberDescriptor.getContainerOfDeserializedMember(): KDeclarationContainerImpl =
    when (val containerSource = containerSource) {
        is JvmPackagePartSource -> (containerSource.knownJvmBinaryClass as? ReflectKotlinClass)?.klass?.let {
            Reflection.getOrCreateKotlinPackage(it) as KPackageImpl
        } ?: throw KotlinReflectionInternalError(
            "Container of top-level deserialized member is not resolved: $this (${containerSource.knownJvmBinaryClass}"
        )
        is LocalDelegatedPropertyFakeContainerSource -> containerSource.container
        is ReflectedLambdaFakeContainerSource -> EmptyContainerForLocal
        else -> throw KotlinReflectionInternalError("Container of deserialized member is not resolved: $this")
    }
