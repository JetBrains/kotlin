/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.load.java.descriptors.JavaCallableMemberDescriptor
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType
import java.util.*
import kotlin.coroutines.Continuation
import kotlin.reflect.*
import kotlin.reflect.jvm.internal.calls.Caller
import kotlin.reflect.jvm.javaType

internal abstract class KCallableImpl<out R> : KCallable<R> {
    abstract val descriptor: CallableMemberDescriptor

    // The instance which is used to perform a positional call, i.e. `call`
    abstract val caller: Caller<*>

    // The instance which is used to perform a call "by name", i.e. `callBy`
    abstract val defaultCaller: Caller<*>?

    abstract val container: KDeclarationContainerImpl

    abstract val isBound: Boolean

    private val _annotations = ReflectProperties.lazySoft { descriptor.computeAnnotations() }

    override val annotations: List<Annotation> get() = _annotations()

    private val _parameters = ReflectProperties.lazySoft {
        val descriptor = descriptor
        val result = ArrayList<KParameter>()
        var index = 0

        if (descriptor.dispatchReceiverParameter != null && !isBound) {
            result.add(KParameterImpl(this, index++, KParameter.Kind.INSTANCE) { descriptor.dispatchReceiverParameter!! })
        }

        if (descriptor.extensionReceiverParameter != null && !isBound) {
            result.add(KParameterImpl(this, index++, KParameter.Kind.EXTENSION_RECEIVER) { descriptor.extensionReceiverParameter!! })
        }

        for (i in descriptor.valueParameters.indices) {
            result.add(KParameterImpl(this, index++, KParameter.Kind.VALUE) { descriptor.valueParameters[i] })
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

    override val parameters: List<KParameter>
        get() = _parameters()

    private val _returnType = ReflectProperties.lazySoft {
        KTypeImpl(descriptor.returnType!!) {
            extractContinuationArgument() ?: caller.returnType
        }
    }

    override val returnType: KType
        get() = _returnType()

    private val _typeParameters = ReflectProperties.lazySoft {
        descriptor.typeParameters.map(::KTypeParameterImpl)
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

    protected val isAnnotationConstructor: Boolean
        get() = name == "<init>" && container.jClass.isAnnotation

    @Suppress("UNCHECKED_CAST")
    override fun call(vararg args: Any?): R = reflectionCall {
        return caller.call(args) as R
    }

    override fun callBy(args: Map<KParameter, Any?>): R {
        return if (isAnnotationConstructor) callAnnotationConstructor(args) else callDefaultMethod(args, null)
    }

    // See ArgumentGenerator#generate
    internal fun callDefaultMethod(args: Map<KParameter, Any?>, continuationArgument: Continuation<*>?): R {
        val parameters = parameters
        val arguments = ArrayList<Any?>(parameters.size)
        var mask = 0
        val masks = ArrayList<Int>(1)
        var index = 0
        var anyOptional = false

        for (parameter in parameters) {
            if (index != 0 && index % Integer.SIZE == 0) {
                masks.add(mask)
                mask = 0
            }

            when {
                args.containsKey(parameter) -> {
                    arguments.add(args[parameter])
                }
                parameter.isOptional -> {
                    arguments.add(defaultPrimitiveValue(parameter.type.javaType))
                    mask = mask or (1 shl (index % Integer.SIZE))
                    anyOptional = true
                }
                else -> {
                    throw IllegalArgumentException("No argument provided for a required parameter: $parameter")
                }
            }

            if (parameter.kind == KParameter.Kind.VALUE) {
                index++
            }
        }

        if (continuationArgument != null) {
            arguments.add(continuationArgument)
        }

        if (!anyOptional) {
            return call(*arguments.toTypedArray())
        }

        masks.add(mask)

        val caller = defaultCaller ?: throw KotlinReflectionInternalError("This callable does not support a default call: $descriptor")

        arguments.addAll(masks)

        // DefaultConstructorMarker or MethodHandle
        arguments.add(null)

        @Suppress("UNCHECKED_CAST")
        return reflectionCall {
            caller.call(arguments.toTypedArray()) as R
        }
    }

    private fun callAnnotationConstructor(args: Map<KParameter, Any?>): R {
        val arguments = parameters.map { parameter ->
            when {
                args.containsKey(parameter) -> {
                    args[parameter] ?: throw IllegalArgumentException("Annotation argument value cannot be null ($parameter)")
                }
                parameter.isOptional -> null
                else -> throw IllegalArgumentException("No argument provided for a required parameter: $parameter")
            }
        }

        val caller = defaultCaller ?: throw KotlinReflectionInternalError("This callable does not support a default call: $descriptor")

        @Suppress("UNCHECKED_CAST")
        return reflectionCall {
            caller.call(arguments.toTypedArray()) as R
        }
    }

    private fun defaultPrimitiveValue(type: Type): Any? =
        if (type is Class<*> && type.isPrimitive) {
            when (type) {
                Boolean::class.java -> false
                Char::class.java -> 0.toChar()
                Byte::class.java -> 0.toByte()
                Short::class.java -> 0.toShort()
                Int::class.java -> 0
                Float::class.java -> 0f
                Long::class.java -> 0L
                Double::class.java -> 0.0
                Void.TYPE -> throw IllegalStateException("Parameter with void type is illegal")
                else -> throw UnsupportedOperationException("Unknown primitive: $type")
            }
        } else null

    private fun extractContinuationArgument(): Type? {
        if ((descriptor as? FunctionDescriptor)?.isSuspend == true) {
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
