/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import kotlin.coroutines.Continuation
import kotlin.jvm.internal.CallableReference
import kotlin.metadata.Modality
import kotlin.reflect.KCallable
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.jvm.internal.calls.Caller
import kotlin.reflect.jvm.internal.calls.getInlineClassUnboxMethod
import kotlin.reflect.jvm.internal.calls.isUnderlyingPropertyOfValueClass
import kotlin.reflect.jvm.internal.calls.toInlineClass
import kotlin.reflect.jvm.javaType
import kotlin.reflect.jvm.jvmErasure
import java.lang.reflect.Array as ReflectArray

/**
 * This interface and its subinterfaces are used to provide operations supported by all implementations of [KCallable] in kotlin-reflect
 * (K1 descriptor-based, kotlin-metadata-jvm, and Java-based) that are not yet exposed in the public API of [KCallable].
 */
internal interface ReflectKCallable<out R> : KCallable<R>, KTypeParameterOwnerImpl {
    val container: KDeclarationContainerImpl

    val rawBoundReceiver: Any?

    /**
     * In contrast to [parameters], includes instance/extension/context parameters, even if the callable is bound.
     */
    val allParameters: List<KParameter>

    /**
     * Instance which is used to perform a positional call, i.e. `call`.
     */
    val caller: Caller<*>

    /**
     * Instance which is used to perform a call "by name", i.e. `callBy`.
     */
    val callerWithDefaults: Caller<*>?

    /**
     * Returns an array that contains default values of all parameter types, which is copied and filled on every `callBy`.
     *
     * @see computeAbsentArguments
     */
    fun getAbsentArguments(): Array<Any?>

    val overriddenStorage: KCallableOverriddenStorage

    val modality: Modality

    val isPackagePrivate: Boolean

    fun shallowCopy(
        container: KDeclarationContainerImpl,
        overriddenStorage: KCallableOverriddenStorage,
    ): ReflectKCallable<R>

    @Suppress("UNCHECKED_CAST")
    override fun call(vararg args: Any?): R = reflectionCall {
        return caller.call(args) as R
    }

    override fun callBy(args: Map<KParameter, Any?>): R {
        return if (isAnnotationConstructor) callAnnotationConstructor(args) else callDefaultMethod(args, null)
    }
}

internal val ReflectKCallable<*>.isBound: Boolean
    get() = rawBoundReceiver !== CallableReference.NO_RECEIVER

/**
 * Same as [ReflectKCallable.rawBoundReceiver], except for when the receiver is an inline class value, in which case it's unboxed.
 */
internal val ReflectKCallable<*>.boundReceiver: Any?
    get() = rawBoundReceiver.coerceToExpectedReceiverType(this)

private fun Any?.coerceToExpectedReceiverType(callable: ReflectKCallable<*>): Any? {
    if (callable is ReflectKProperty<*> && callable.isUnderlyingPropertyOfValueClass()) return this

    val expectedReceiverType = callable.allParameters.singleOrNull { it.kind != KParameter.Kind.VALUE }?.type
    val unboxMethod = expectedReceiverType?.toInlineClass()?.getInlineClassUnboxMethod(callable) ?: return this

    return unboxMethod.invoke(this)
}

internal fun ReflectKCallable<*>.computeAbsentArguments(): Array<Any?> {
    val parameters = parameters
    val parameterSize = parameters.size + (if (isSuspend) 1 else 0)

    val parametersWithAllocatedBitInMask = parameters.count { it.kind == KParameter.Kind.VALUE || it.kind == KParameter.Kind.CONTEXT }
    val maskSize = (parametersWithAllocatedBitInMask + Integer.SIZE - 1) / Integer.SIZE

    // Array containing the actual function arguments, masks, and +1 for DefaultConstructorMarker or MethodHandle.
    val arguments = arrayOfNulls<Any?>(parameterSize + maskSize + 1)

    // Set values of primitive (and inline class) arguments to the boxed default values (such as 0, 0.0, false) instead of nulls.
    parameters.forEach { parameter ->
        if (parameter.isOptional && !parameter.type.isInlineClassType) {
            // For inline class types, the javaType refers to the underlying type of the inline class,
            // but we have to pass null in order to mark the argument as absent for ValueClassAwareCaller.
            arguments[parameter.index] = defaultPrimitiveValue(parameter.type.javaType)
        } else if (parameter.isVararg) {
            arguments[parameter.index] = defaultEmptyArray(parameter.type)
        }
    }

    for (i in 0 until maskSize) {
        arguments[parameterSize + i] = 0
    }

    return arguments
}

internal fun <R> ReflectKCallable<R>.callDefaultMethod(args: Map<KParameter, Any?>, continuationArgument: Continuation<*>?): R {
    val parameters = parameters

    // Optimization for functions without value/receiver parameters.
    if (parameters.isEmpty()) {
        @Suppress("UNCHECKED_CAST")
        return reflectionCall {
            caller.call(if (isSuspend) arrayOf(continuationArgument) else emptyArray()) as R
        }
    }

    val parameterSize = parameters.size + (if (isSuspend) 1 else 0)

    val arguments = getAbsentArguments().apply {
        if (isSuspend) {
            this[parameters.size] = continuationArgument
        }
    }

    var valueParameterIndex = 0
    var anyOptional = false

    for (parameter in parameters) {
        when {
            args.containsKey(parameter) -> {
                arguments[parameter.index] = args[parameter]
            }
            parameter.isOptional -> {
                val maskIndex = parameterSize + (valueParameterIndex / Integer.SIZE)
                arguments[maskIndex] = (arguments[maskIndex] as Int) or (1 shl (valueParameterIndex % Integer.SIZE))
                anyOptional = true
            }
            parameter.isVararg -> {}
            else -> {
                throw IllegalArgumentException("No argument provided for a required parameter: $parameter")
            }
        }
        if (parameter.kind == KParameter.Kind.VALUE || parameter.kind == KParameter.Kind.CONTEXT) {
            valueParameterIndex++
        }
    }

    if (!anyOptional) {
        @Suppress("UNCHECKED_CAST")
        return reflectionCall {
            caller.call(arguments.copyOf(parameterSize)) as R
        }
    }

    val caller = callerWithDefaults ?: throw KotlinReflectionInternalError("This callable does not support a default call: $this")

    @Suppress("UNCHECKED_CAST")
    return reflectionCall {
        caller.call(arguments) as R
    }
}

internal fun <R> ReflectKCallable<R>.callAnnotationConstructor(args: Map<KParameter, Any?>): R {
    val arguments = parameters.map { parameter ->
        when {
            args.containsKey(parameter) -> {
                args[parameter] ?: throw IllegalArgumentException("Annotation argument value cannot be null ($parameter)")
            }
            parameter.isOptional -> null
            parameter.isVararg -> defaultEmptyArray(parameter.type)
            else -> throw IllegalArgumentException("No argument provided for a required parameter: $parameter")
        }
    }

    val caller = callerWithDefaults ?: throw KotlinReflectionInternalError("This callable does not support a default call: $this")

    @Suppress("UNCHECKED_CAST")
    return reflectionCall {
        caller.call(arguments.toTypedArray()) as R
    }
}

private fun defaultEmptyArray(type: KType): Any =
    type.jvmErasure.java.run {
        if (isArray) ReflectArray.newInstance(componentType, 0)
        else throw KotlinReflectionInternalError(
            "Cannot instantiate the default empty array of type $simpleName, because it is not an array type"
        )
    }

internal val ReflectKCallable<*>.isConstructor: Boolean
    get() = name == "<init>"

internal val ReflectKCallable<*>.isAnnotationConstructor: Boolean
    get() = isConstructor && container.jClass.isAnnotation
