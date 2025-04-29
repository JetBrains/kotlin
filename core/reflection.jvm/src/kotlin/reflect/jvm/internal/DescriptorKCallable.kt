/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.load.java.descriptors.JavaCallableMemberDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPropertyDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedSimpleFunctionDescriptor
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType
import kotlin.coroutines.Continuation
import kotlin.reflect.*
import kotlin.reflect.jvm.internal.types.DescriptorKType

internal abstract class DescriptorKCallable<out R> : ReflectKCallable<R> {
    abstract val descriptor: CallableMemberDescriptor

    private val _annotations = ReflectProperties.lazySoft { descriptor.computeAnnotations() }

    override val annotations: List<Annotation> get() = _annotations()

    private val _receiverParameters = ReflectProperties.lazySoft {
        val result = ArrayList<KParameter>()
        val instanceReceiver = descriptor.instanceReceiverParameter
        if (instanceReceiver != null) {
            result.add(DescriptorKParameter(this, result.size, KParameter.Kind.INSTANCE) { instanceReceiver })
        }

        val contextParameters = descriptor.computeContextParameters()
        for (i in contextParameters.indices) {
            @OptIn(ExperimentalContextParameters::class)
            result.add(DescriptorKParameter(this, result.size, KParameter.Kind.CONTEXT) { contextParameters[i] })
        }

        val extensionReceiver = descriptor.extensionReceiverParameter
        if (extensionReceiver != null) {
            result.add(DescriptorKParameter(this, result.size, KParameter.Kind.EXTENSION_RECEIVER) { extensionReceiver })
        }
        result
    }

    override val receiverParameters: List<KParameter> get() = _receiverParameters()

    private val _parameters = ReflectProperties.lazySoft {
        val descriptor = descriptor
        val result = ArrayList<KParameter>()

        if (!isBound) {
            result.addAll(receiverParameters)
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
        result
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

    override val parameters: List<KParameter>
        get() = _parameters()

    private val _returnType = ReflectProperties.lazySoft {
        DescriptorKType(descriptor.returnType!!) {
            extractContinuationArgument() ?: caller.returnType
        }
    }

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

    private fun extractContinuationArgument(): Type? {
        if (isSuspend) {
            // kotlin.coroutines.Continuation<? super java.lang.String>
            val continuationType = caller.parameterTypes.lastOrNull() as? ParameterizedType
            if (continuationType?.rawType == Continuation::class.java) {
                // ? super java.lang.String
                val wildcard = continuationType.actualTypeArguments.single() as? WildcardType
                // java.lang.String
                return wildcard?.lowerBounds?.first()
            }
        }

        return null
    }
}
