/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal.calls

import org.jetbrains.kotlin.load.java.JvmAbi
import java.lang.reflect.Member
import java.lang.reflect.Method
import java.lang.reflect.Type
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.full.createDefaultType
import kotlin.reflect.full.withNullability
import kotlin.reflect.jvm.internal.*

/**
 * A caller that is used whenever the declaration has value classes in its parameter types or inline class in return type.
 * Each argument of a value class type is unboxed, and the return value (if it's of an inline class type) is boxed.
 */
internal class ValueClassAwareCaller<out M : Member?>(
    callable: ReflectKCallable<*>,
    private val caller: Caller<M>,
    private val isDefault: Boolean,
) : Caller<M> {
    override val member: M
        get() = caller.member

    override val returnType: Type
        get() = caller.returnType

    override val parameterTypes: List<Type>
        get() = caller.parameterTypes

    override val isBoundInstanceCallWithValueClasses: Boolean
        get() = caller is CallerImpl.Method.BoundInstance

    private class BoxUnboxData(val argumentRange: IntRange, val unboxParameters: Array<Method?>, val box: Method?)

    private val data: BoxUnboxData = run {
        val returnType = callable.returnType
        val box = if (callable is ReflectKFunction && callable.isSuspend &&
            returnType.unsubstitutedUnderlyingType()?.isPrimitiveType() == true
        ) {
            // Suspend functions always return java.lang.Object, and value classes over primitives are already boxed there.
            null
        } else {
            returnType.toInlineClass()?.getBoxMethod(callable)
        }

        if (callable.isGetterOfUnderlyingPropertyOfValueClass()) {
            // Getter of the underlying val of a value class is always called on a boxed receiver,
            // no argument unboxing is required.
            return@run BoxUnboxData(IntRange.EMPTY, emptyArray(), box)
        }

        val shift = when {
            caller is CallerImpl.Method.BoundStatic && !caller.isCallByToValueClassMangledMethod -> {
                // Bound reference to a static method is only possible for a top level extension function/property,
                // and in that case the number of expected arguments is one less than usual, hence -1
                -1
            }

            callable.isConstructor ->
                if (caller is BoundCaller) -1 else 0

            callable.parameters.any { it.kind == KParameter.Kind.INSTANCE } -> {
                // If we have an unbound reference to the value class member,
                // its receiver (which is passed as argument 0) should also be unboxed.
                if ((callable.container as? KClassImpl<*>)?.isValue == true)
                    0
                else
                    1
            }

            else -> 0
        }

        val kotlinParameterTypes = makeKotlinParameterTypes(callable, caller.member)

        val paramsWithAllocatedDefaultMaskBitsCount = if (callable.allParameters.any { it.kind == KParameter.Kind.EXTENSION_RECEIVER }) {
            kotlinParameterTypes.size - 1
        } else {
            kotlinParameterTypes.size
        }

        // If the default argument is set,
        // (paramsWithAllocatedDefaultMaskBitsCount + Int.SIZE_BITS - 1) / Int.SIZE_BITS masks and one marker are added to the end of the argument.
        val extraArgumentsTail =
            (if (isDefault) ((paramsWithAllocatedDefaultMaskBitsCount + Int.SIZE_BITS - 1) / Int.SIZE_BITS) + 1 else 0) +
                    (if (callable is ReflectKFunction && callable.isSuspend) 1 else 0)
        val expectedArgsSize = kotlinParameterTypes.size + shift + extraArgumentsTail
        checkParametersSize(expectedArgsSize, callable, isDefault)

        // maxOf is needed because in case of a bound top level extension, shift can be -1 (see above). But in that case, we need not unbox
        // the extension receiver argument, since it has already been unboxed at compile time and generated into the reference
        val argumentRange = maxOf(shift, 0) until (kotlinParameterTypes.size + shift)

        val unbox = Array(expectedArgsSize) { i ->
            if (i in argumentRange)
                kotlinParameterTypes[i - shift].toInlineClass()?.getInlineClassUnboxMethod(callable)
            else null
        }

        BoxUnboxData(argumentRange, unbox, box)
    }

    override fun call(args: Array<*>): Any? {
        val range = data.argumentRange
        val unbox = data.unboxParameters
        val box = data.box

        val unboxedArguments = Array(args.size) { index ->
            val arg = args[index]
            if (index in range) {
                // Note that arg may be null in case we're calling a $default method, and it's an optional parameter of an inline class type
                val method = unbox[index]
                when {
                    method == null -> arg
                    arg != null -> method.invoke(arg)
                    else -> defaultPrimitiveValue(method.returnType)
                }
            } else {
                arg
            }
        }

        val result = caller.call(unboxedArguments)
        if (result === COROUTINE_SUSPENDED) return result

        return box?.invoke(null, result) ?: result
    }
}

private fun Caller<*>.checkParametersSize(expectedArgsSize: Int, callable: ReflectKCallable<*>, isDefault: Boolean) {
    if (arity != expectedArgsSize) {
        throw KotlinReflectionInternalError(
            "Inconsistent number of parameters in the descriptor and Java reflection object: $arity != $expectedArgsSize\n" +
                    "Calling: $callable\n" +
                    "Parameter types: ${this.parameterTypes})\n" +
                    "Default: $isDefault"
        )
    }
}

private fun makeKotlinParameterTypes(callable: ReflectKCallable<*>, member: Member?): List<KType> {
    val result = mutableListOf<KType>()
    val container = callable.container
    if (!callable.isConstructor && container is KClass<*> && container.isValue) {
        val containerType = container.createDefaultType()
        if (member?.acceptsBoxedReceiverParameter() == true) {
            // Hack to forbid unboxing dispatchReceiver if it is used upcasted.
            // `kotlinParameterTypes` are used to determine shifts and calls according to whether type is an inline class or not.
            // If it is an inline class, boxes are unboxed. If the actual called member lies in the interface/DefaultImpls class,
            // it accepts a boxed parameter as ex-dispatch receiver. Making the type nullable allows to prevent unboxing in this case.
            result.add(containerType.withNullability(nullable = true))
        } else {
            result.add(containerType)
        }
    }

    val isInnerClassConstructor = callable.isConstructor && (container as? KClass<*>)?.isInner == true

    for (parameter in callable.allParameters) {
        if (parameter.kind != KParameter.Kind.INSTANCE || isInnerClassConstructor) {
            result.add(parameter.type)
        }
    }

    return result
}

private fun Member.acceptsBoxedReceiverParameter(): Boolean {
    // Method implementation can be placed either in
    //  * the value class itself,
    //  * interface$DefaultImpls, 
    //  * interface default method (Java 8+).
    // Here we need to understand that it is the second or the third case. Both of the cases cannot be value classes,
    // so the simplest solution is to check declaringClass for being a value class.
    val clazz = declaringClass ?: return false
    return !clazz.kotlin.isValue
}

internal fun <M : Member?> Caller<M>.createValueClassAwareCallerIfNeeded(callable: ReflectKCallable<*>, isDefault: Boolean): Caller<M> =
    if (callable.parameters.any { it.type.isInlineClassType } || callable.returnType.isInlineClassType)
        ValueClassAwareCaller(callable, this, isDefault)
    else this

internal fun Class<*>.getInlineClassUnboxMethod(callable: ReflectKCallable<*>): Method =
    try {
        getDeclaredMethod("unbox" + JvmAbi.IMPL_SUFFIX_FOR_INLINE_CLASS_MEMBERS)
    } catch (e: NoSuchMethodException) {
        throw KotlinReflectionInternalError("No unbox method found in inline class: $this (calling $callable)")
    }

private fun Class<*>.getBoxMethod(callable: ReflectKCallable<*>): Method =
    try {
        getDeclaredMethod("box" + JvmAbi.IMPL_SUFFIX_FOR_INLINE_CLASS_MEMBERS, getInlineClassUnboxMethod(callable).returnType)
    } catch (e: NoSuchMethodException) {
        throw KotlinReflectionInternalError("No box method found in inline class: $this (calling $callable)")
    }

internal fun KType?.toInlineClass(): Class<*>? {
    // See computeExpandedTypeForInlineClass.
    val klass = this?.classifier as? KClass<*> ?: return null
    if (!klass.isValue) return null
    if (!isNullableType()) return klass.java

    val expandedUnderlyingType = unsubstitutedUnderlyingType() ?: return null
    if (!expandedUnderlyingType.isNullableType() && !expandedUnderlyingType.isPrimitiveType()) return klass.java

    return null
}

private fun ReflectKCallable<*>.isGetterOfUnderlyingPropertyOfValueClass(): Boolean =
    this is KProperty.Getter<*> && (property as ReflectKProperty<*>).isUnderlyingPropertyOfValueClass()

private fun ReflectKProperty<*>.isUnderlyingPropertyOfValueClass(): Boolean =
    allParameters.all { it.kind == KParameter.Kind.INSTANCE } &&
            name == (container as? KClassImpl<*>)?.inlineClassUnderlyingPropertyName

private fun KType.isPrimitiveType(): Boolean {
    if (isMarkedNullable) return false
    val klass = (classifier as? KClass<*>)?.javaPrimitiveType
    return klass != null && klass != Void.TYPE
}

private fun KType.unsubstitutedUnderlyingType(): KType? =
    (classifier as? KClassImpl<*>)?.inlineClassUnderlyingType
