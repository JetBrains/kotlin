/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.load.java.descriptors.JavaCallableMemberDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPropertyDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedSimpleFunctionDescriptor
import kotlin.reflect.*
import kotlin.reflect.jvm.internal.types.DescriptorKType

internal abstract class DescriptorKCallable<out R> : ReflectKCallable<R> {
    abstract val descriptor: CallableMemberDescriptor

    protected abstract fun computeReturnType(): DescriptorKType

    private val _annotations = ReflectProperties.lazySoft { descriptor.computeAnnotations() }

    override val annotations: List<Annotation> get() = _annotations()

    private val _allParameters = ReflectProperties.lazySoft { computeParameters(includeReceivers = true) }

    override val allParameters: List<KParameter> get() = _allParameters()

    private val _parameters = ReflectProperties.lazySoft {
        if (isBound) computeParameters(includeReceivers = false) else allParameters
    }

    override val parameters: List<KParameter> get() = _parameters()

    private fun computeParameters(includeReceivers: Boolean): List<KParameter> {
        val descriptor = descriptor
        val result = ArrayList<KParameter>()
        if (includeReceivers) {
            val instanceReceiver = descriptor.instanceReceiverParameter
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

    private val _returnType = ReflectProperties.lazySoft { computeReturnType() }

    override val returnType: KType
        get() = _returnType()

    private val _typeParameters = ReflectProperties.lazySoft {
        descriptor.typeParameters.map { descriptor -> KTypeParameterImpl(this, descriptor) }
    }

    override val typeParameters: List<KTypeParameter>
        get() = _typeParameters()

    override val visibility: KVisibility?
        get() = descriptor.visibility.toKVisibility()

    override val isFinal: Boolean
        get() = descriptor.modality == Modality.FINAL

    override val isOpen: Boolean
        get() = descriptor.modality == Modality.OPEN

    override val isAbstract: Boolean
        get() = descriptor.modality == Modality.ABSTRACT

    private val _absentArguments = ReflectProperties.lazySoft(::computeAbsentArguments)

    override fun getAbsentArguments(): Array<Any?> = _absentArguments().clone()
}
