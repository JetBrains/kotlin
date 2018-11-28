/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal.calls

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.isGetterOfUnderlyingPropertyOfInlineClass
import org.jetbrains.kotlin.resolve.isInlineClass
import org.jetbrains.kotlin.resolve.isInlineClassType
import org.jetbrains.kotlin.resolve.isUnderlyingPropertyOfInlineClass
import org.jetbrains.kotlin.types.KotlinType
import java.lang.reflect.Member
import java.lang.reflect.Method
import java.lang.reflect.Type
import kotlin.reflect.jvm.internal.KotlinReflectionInternalError
import kotlin.reflect.jvm.internal.toJavaClass

/**
 * A caller that is used whenever the declaration has inline classes in its parameter types or return type.
 * Each argument of an inline class type is unboxed, and the return value (if it's of an inline class type) is boxed.
 */
internal class InlineClassAwareCaller<out M : Member?>(
    descriptor: CallableMemberDescriptor,
    private val caller: Caller<M>,
    private val isDefault: Boolean
) : Caller<M> {
    override val member: M
        get() = caller.member

    override val returnType: Type
        get() = caller.returnType

    override val parameterTypes: List<Type>
        get() = caller.parameterTypes

    private class BoxUnboxData(val argumentRange: IntRange, val unbox: Array<Method?>, val box: Method?) {
        operator fun component1(): IntRange = argumentRange
        operator fun component2(): Array<Method?> = unbox
        operator fun component3(): Method? = box
    }

    private val data: BoxUnboxData = run {
        val box = descriptor.returnType!!.toInlineClass()?.getBoxMethod(descriptor)

        if (descriptor.isGetterOfUnderlyingPropertyOfInlineClass()) {
            // Getter of the underlying val of an inline class is always called on a boxed receiver,
            // no argument boxing/unboxing is required.
            // However, its result might require boxing if it is an inline class type.
            return@run BoxUnboxData(IntRange.EMPTY, emptyArray(), box)
        }

        val shift = when {
            caller is CallerImpl.Method.BoundStatic -> {
                // Bound reference to a static method is only possible for a top level extension function/property,
                // and in that case the number of expected arguments is one less than usual, hence -1
                -1
            }

            descriptor is ConstructorDescriptor ->
                if (caller is BoundCaller) -1 else 0

            descriptor.dispatchReceiverParameter != null && caller !is BoundCaller -> {
                // If we have an unbound reference to the inline class member,
                // its receiver (which is passed as argument 0) should also be unboxed.
                if (descriptor.containingDeclaration.isInlineClass())
                    0
                else
                    1
            }

            else -> 0
        }

        val extraArgumentsTail = if (isDefault) 2 else 0

        val kotlinParameterTypes: List<KotlinType> = ArrayList<KotlinType>().also { kotlinParameterTypes ->
            val extensionReceiverType = descriptor.extensionReceiverParameter?.type
            if (extensionReceiverType != null) {
                kotlinParameterTypes.add(extensionReceiverType)
            } else if (descriptor is ConstructorDescriptor) {
                val constructedClass = descriptor.constructedClass
                if (constructedClass.isInner) {
                    kotlinParameterTypes.add((constructedClass.containingDeclaration as ClassDescriptor).defaultType)
                }
            } else {
                val containingDeclaration = descriptor.containingDeclaration
                if (containingDeclaration is ClassDescriptor && containingDeclaration.isInline) {
                    kotlinParameterTypes.add(containingDeclaration.defaultType)
                }
            }

            descriptor.valueParameters.mapTo(kotlinParameterTypes, ValueParameterDescriptor::getType)
        }
        val expectedArgsSize = kotlinParameterTypes.size + shift + extraArgumentsTail
        if (arity != expectedArgsSize) {
            throw KotlinReflectionInternalError(
                "Inconsistent number of parameters in the descriptor and Java reflection object: $arity != $expectedArgsSize\n" +
                        "Calling: $descriptor\n" +
                        "Parameter types: ${this.parameterTypes})\n" +
                        "Default: $isDefault"
            )
        }

        // maxOf is needed because in case of a bound top level extension, shift can be -1 (see above). But in that case, we need not unbox
        // the extension receiver argument, since it has already been unboxed at compile time and generated into the reference
        val argumentRange = maxOf(shift, 0) until (kotlinParameterTypes.size + shift)

        val unbox = Array(expectedArgsSize) { i ->
            if (i in argumentRange) {
                kotlinParameterTypes[i - shift].toInlineClass()?.getUnboxMethod(descriptor)
            } else null
        }

        BoxUnboxData(argumentRange, unbox, box)
    }

    override fun call(args: Array<*>): Any? {
        val (range, unbox, box) = data

        @Suppress("UNCHECKED_CAST")
        val unboxed = args.copyOf() as Array<Any?>
        for (index in range) {
            val method = unbox[index]
            val arg = args[index]
            // Note that arg may be null in case we're calling a $default method and it's an optional parameter of an inline class type
            unboxed[index] =
                    if (method != null && arg != null) method.invoke(arg)
                    else arg
        }

        val result = caller.call(unboxed)

        return box?.invoke(null, result) ?: result
    }
}

internal fun <M : Member?> Caller<M>.createInlineClassAwareCallerIfNeeded(
    descriptor: CallableMemberDescriptor,
    isDefault: Boolean = false
): Caller<M> {
    val needsInlineAwareCaller: Boolean =
        descriptor.isGetterOfUnderlyingPropertyOfInlineClass() ||
                descriptor.valueParameters.any { it.type.isInlineClassType() } ||
                descriptor.returnType?.isInlineClassType() == true ||
                this !is BoundCaller && descriptor.hasInlineClassReceiver()

    return if (needsInlineAwareCaller) InlineClassAwareCaller(descriptor, this, isDefault) else this
}

private fun CallableMemberDescriptor.hasInlineClassReceiver() =
    expectedReceiverType?.isInlineClassType() == true

internal fun Class<*>.getUnboxMethod(descriptor: CallableMemberDescriptor): Method =
    try {
        getDeclaredMethod("unbox" + JvmAbi.IMPL_SUFFIX_FOR_INLINE_CLASS_MEMBERS)
    } catch (e: NoSuchMethodException) {
        throw KotlinReflectionInternalError("No unbox method found in inline class: $this (calling $descriptor)")
    }

internal fun Class<*>.getBoxMethod(descriptor: CallableMemberDescriptor): Method =
    try {
        getDeclaredMethod("box" + JvmAbi.IMPL_SUFFIX_FOR_INLINE_CLASS_MEMBERS, getUnboxMethod(descriptor).returnType)
    } catch (e: NoSuchMethodException) {
        throw KotlinReflectionInternalError("No box method found in inline class: $this (calling $descriptor)")
    }

internal fun KotlinType.toInlineClass(): Class<*>? =
    constructor.declarationDescriptor.toInlineClass()

internal fun DeclarationDescriptor?.toInlineClass(): Class<*>? =
    if (this is ClassDescriptor && isInline)
        toJavaClass() ?: throw KotlinReflectionInternalError("Class object for the class $name cannot be found (classId=$classId)")
    else
        null

private val CallableMemberDescriptor.expectedReceiverType: KotlinType?
    get() {
        val extensionReceiver = extensionReceiverParameter
        val dispatchReceiver = dispatchReceiverParameter
        return when {
            extensionReceiver != null -> extensionReceiver.type
            dispatchReceiver == null -> null
            this is ConstructorDescriptor -> dispatchReceiver.type
            else -> (containingDeclaration as? ClassDescriptor)?.defaultType
        }
    }

internal fun Any?.coerceToExpectedReceiverType(descriptor: CallableMemberDescriptor): Any? {
    if (descriptor is PropertyDescriptor && descriptor.isUnderlyingPropertyOfInlineClass()) return this

    val expectedReceiverType = descriptor.expectedReceiverType
    val unboxMethod = expectedReceiverType?.toInlineClass()?.getUnboxMethod(descriptor) ?: return this

    return unboxMethod.invoke(this)
}
