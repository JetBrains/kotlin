/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.load.java.JavaDescriptorVisibilities
import org.jetbrains.kotlin.load.java.descriptors.JavaCallableMemberDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPropertyDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedSimpleFunctionDescriptor
import kotlin.metadata.Modality
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.KVisibility
import kotlin.reflect.jvm.internal.types.DescriptorKType
import org.jetbrains.kotlin.descriptors.Modality as DescriptorModality

internal abstract class DescriptorKCallable<out R>(
    overriddenStorage: KCallableOverriddenStorage,
) : ReflectKCallableImpl<R>(overriddenStorage) {
    abstract val descriptor: CallableMemberDescriptor

    protected abstract fun computeReturnType(): DescriptorKType

    private val _annotations = ReflectProperties.lazySoft { descriptor.computeAnnotations() }

    override val annotations: List<Annotation> get() = _annotations()

    private val _allParameters = ReflectProperties.lazySoft { computeParameters(includeReceivers = true) }

    override val allParameters: List<KParameter> get() = _allParameters()

    private val _parameters = ReflectProperties.lazySoft {
        if (isBound) computeParameters(includeReceivers = false) else allParameters
    }

    final override val parameters: List<KParameter> get() = _parameters()

    private fun computeParameters(includeReceivers: Boolean): List<KParameter> {
        val descriptor = descriptor
        val result = ArrayList<KParameter>()
        if (includeReceivers) {
            val instanceReceiver = instanceReceiverParameter
            if (instanceReceiver != null) {
                result.add(DescriptorKParameter(this, result.size, KParameter.Kind.INSTANCE) { instanceReceiver })
            }

            val contextParameters = descriptor.computeContextParameters()
            for (i in contextParameters.indices) {
                result.add(DescriptorKParameter(this, result.size, KParameter.Kind.CONTEXT) { contextParameters[i] })
            }

            val extensionReceiver = descriptor.extensionReceiverParameter
            if (extensionReceiver != null) {
                result.add(DescriptorKParameter(this, result.size, KParameter.Kind.EXTENSION_RECEIVER) { extensionReceiver })
            }
        }

        for (i in descriptor.valueParameters.indices) {
            result.add(DescriptorKParameter(this, result.size, KParameter.Kind.VALUE) { descriptor.valueParameters[i] })
        }

        // Constructor parameters of Java annotations are not ordered in any way, we order them by name here to be more stable.
        // Note that positional call (via "call") is not allowed unless there's a single non-"value" parameter,
        // so the order of parameters of Java annotation constructors here can be arbitrary
        if (isAnnotationConstructor && descriptor is JavaCallableMemberDescriptor) {
            result.sortBy { it.name }
        }

        result.trimToSize()
        return result
    }

    private fun CallableMemberDescriptor.computeContextParameters(): List<ValueParameterDescriptor> {
        val (nameResolver, contextParameters) = when (this) {
            is DeserializedSimpleFunctionDescriptor -> nameResolver to proto.contextParameterList
            is DeserializedPropertyDescriptor -> nameResolver to proto.contextParameterList
            is PropertyAccessorDescriptor -> (correspondingProperty as? DeserializedPropertyDescriptor)?.let {
                it.nameResolver to it.proto.contextParameterList
            }
            else -> null
        } ?: return emptyList()
        return contextReceiverParameters.mapIndexed { index, parameter ->
            ValueParameterDescriptorImpl(
                this, null, index,
                parameter.annotations,
                Name.guessByFirstCharacter(nameResolver.getString(contextParameters[index].name)),
                parameter.type,
                declaresDefaultValue = false,
                isCrossinline = false,
                isNoinline = false,
                varargElementType = null,
                parameter.source,
            )
        }
    }

    private val _returnType = ReflectProperties.lazySoft {
        val type = computeReturnType()
        overriddenStorage.getTypeSubstitutor(typeParameters, memberNameForDebug = name).substitute(type).type
            ?: starProjectionInTopLevelTypeIsNotPossible(containerForDebug = name)
    }

    final override val returnType: KType
        get() = _returnType()

    private val _typeParameters = ReflectProperties.lazySoft {
        val typeParametersWithNotYetSubstitutedUpperBounds =
            descriptor.typeParameters.map { descriptor -> KTypeParameterImpl(this, descriptor) }
        val substitutor = overriddenStorage.getTypeSubstitutor(typeParametersWithNotYetSubstitutedUpperBounds, memberNameForDebug = name)
        for (typeParameter in typeParametersWithNotYetSubstitutedUpperBounds) {
            typeParameter.upperBounds = typeParameter.upperBounds.map { type ->
                substitutor.substitute(type).type ?: starProjectionInTopLevelTypeIsNotPossible(containerForDebug = container)
            }
        }
        typeParametersWithNotYetSubstitutedUpperBounds
    }

    override val typeParameters: List<KTypeParameter>
        get() = _typeParameters()

    override val visibility: KVisibility?
        get() = descriptor.visibility.toKVisibility()

    final override val modality: Modality
        get() = overriddenStorage.modality ?: descriptor.modality.toMetadataModality()

    final override val isPackagePrivate: Boolean
        get() = descriptor.visibility == JavaDescriptorVisibilities.PACKAGE_VISIBILITY
}

private fun DescriptorModality.toMetadataModality(): Modality = when (this) {
    DescriptorModality.FINAL -> Modality.FINAL
    DescriptorModality.OPEN -> Modality.OPEN
    DescriptorModality.ABSTRACT -> Modality.ABSTRACT
    DescriptorModality.SEALED -> Modality.SEALED
}
