/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal.calls

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.typeUtil.makeNullable
import java.lang.reflect.Member
import java.lang.reflect.Method
import java.lang.reflect.Type
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.reflect.jvm.internal.KotlinReflectionInternalError
import kotlin.reflect.jvm.internal.defaultPrimitiveValue
import kotlin.reflect.jvm.internal.toJavaClass

/**
 * A caller that is used whenever the declaration has value classes in its parameter types or inline class in return type.
 * Each argument of a value class type is unboxed, and the return value (if it's of an inline class type) is boxed.
 */
internal class ValueClassAwareCaller<out M : Member?>(
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

    override val isBoundInstanceCallWithValueClasses: Boolean
        get() = caller is CallerImpl.Method.BoundInstance

    private class BoxUnboxData(val argumentRange: IntRange, val unboxParameters: Array<Method?>, val box: Method?)

    private val data: BoxUnboxData = run {
        val returnType = descriptor.returnType!!
        val box = if (
            descriptor is FunctionDescriptor && descriptor.isSuspend &&
            returnType.unsubstitutedUnderlyingType()?.let { KotlinBuiltIns.isPrimitiveType(it) } == true
        ) {
            // Suspend functions always return java.lang.Object, and value classes over primitives are already boxed there.
            null
        } else {
            returnType.toInlineClass()?.getBoxMethod(descriptor)
        }

        if (descriptor.isGetterOfUnderlyingPropertyOfValueClass()) {
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

            descriptor is ConstructorDescriptor ->
                if (caller is BoundCaller) -1 else 0

            descriptor.dispatchReceiverParameter != null && caller !is BoundCaller -> {
                // If we have an unbound reference to the value class member,
                // its receiver (which is passed as argument 0) should also be unboxed.
                if (descriptor.containingDeclaration.isValueClass())
                    0
                else
                    1
            }

            else -> 0
        }

        val kotlinParameterTypes = makeKotlinParameterTypes(descriptor, caller.member)

        // If the default argument is set,
        // (kotlinParameterTypes.size + Int.SIZE_BITS - 1) / Int.SIZE_BITS masks and one marker are added to the end of the argument.
        val extraArgumentsTail =
            (if (isDefault) ((kotlinParameterTypes.size + Int.SIZE_BITS - 1) / Int.SIZE_BITS) + 1 else 0) +
                    (if (descriptor is FunctionDescriptor && descriptor.isSuspend) 1 else 0)
        val expectedArgsSize = kotlinParameterTypes.size + shift + extraArgumentsTail
        checkParametersSize(expectedArgsSize, descriptor, isDefault)

        // maxOf is needed because in case of a bound top level extension, shift can be -1 (see above). But in that case, we need not unbox
        // the extension receiver argument, since it has already been unboxed at compile time and generated into the reference
        val argumentRange = maxOf(shift, 0) until (kotlinParameterTypes.size + shift)

        val unbox = Array(expectedArgsSize) { i ->
            if (i in argumentRange)
                kotlinParameterTypes[i - shift].toInlineClass()?.getInlineClassUnboxMethod(descriptor)
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

private fun Caller<*>.checkParametersSize(
    expectedArgsSize: Int,
    descriptor: CallableMemberDescriptor,
    isDefault: Boolean,
) {
    if (arity != expectedArgsSize) {
        throw KotlinReflectionInternalError(
            "Inconsistent number of parameters in the descriptor and Java reflection object: $arity != $expectedArgsSize\n" +
                    "Calling: $descriptor\n" +
                    "Parameter types: ${this.parameterTypes})\n" +
                    "Default: $isDefault"
        )
    }
}

private fun makeKotlinParameterTypes(descriptor: CallableMemberDescriptor, member: Member?): List<KotlinType> {
    val result = mutableListOf<KotlinType>()
    if (descriptor is ConstructorDescriptor) {
        val constructedClass = descriptor.constructedClass
        if (constructedClass.isInner) {
            result.add((constructedClass.containingDeclaration as ClassDescriptor).defaultType)
        }
    } else {
        val containingDeclaration = descriptor.containingDeclaration
        if (containingDeclaration is ClassDescriptor && containingDeclaration.isValueClass()) {
            if (member?.acceptsBoxedReceiverParameter() == true) {
                // Hack to forbid unboxing dispatchReceiver if it is used upcasted.
                // `kotlinParameterTypes` are used to determine shifts and calls according to whether type is an inline class or not.
                // If it is an inline class, boxes are unboxed. If the actual called member lies in the interface/DefaultImpls class,
                // it accepts a boxed parameter as ex-dispatch receiver. Making the type nullable allows to prevent unboxing in this case.
                result.add(containingDeclaration.defaultType.makeNullable())
            } else {
                result.add(containingDeclaration.defaultType)
            }
        }
        descriptor.extensionReceiverParameter?.type?.let(result::add)
        descriptor.contextReceiverParameters.mapTo(result) { it.type }
    }

    descriptor.valueParameters.mapTo(result, ValueParameterDescriptor::getType)

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

internal fun <M : Member?> Caller<M>.createValueClassAwareCallerIfNeeded(
    descriptor: CallableMemberDescriptor,
    isDefault: Boolean = false,
): Caller<M> {
    val needsValueClassAwareCaller: Boolean =
        descriptor.dispatchReceiverParameter?.type?.isValueClassType() == true ||
                descriptor.extensionReceiverParameter?.type?.isValueClassType() == true ||
                descriptor.contextReceiverParameters.any { it.type.isValueClassType() } ||
                descriptor.valueParameters.any { it.type.isValueClassType() } ||
                descriptor.returnType?.isInlineClassType() == true

    return if (needsValueClassAwareCaller) ValueClassAwareCaller(descriptor, this, isDefault) else this
}

internal fun Class<*>.getInlineClassUnboxMethod(descriptor: CallableMemberDescriptor): Method =
    try {
        getDeclaredMethod("unbox" + JvmAbi.IMPL_SUFFIX_FOR_INLINE_CLASS_MEMBERS)
    } catch (e: NoSuchMethodException) {
        throw KotlinReflectionInternalError("No unbox method found in inline class: $this (calling $descriptor)")
    }

private fun Class<*>.getBoxMethod(descriptor: CallableMemberDescriptor): Method =
    try {
        getDeclaredMethod("box" + JvmAbi.IMPL_SUFFIX_FOR_INLINE_CLASS_MEMBERS, getInlineClassUnboxMethod(descriptor).returnType)
    } catch (e: NoSuchMethodException) {
        throw KotlinReflectionInternalError("No box method found in inline class: $this (calling $descriptor)")
    }

private fun KotlinType.toInlineClass(): Class<*>? {
    // See computeExpandedTypeForInlineClass.
    // TODO: add tests on type parameters with value class bounds.
    // TODO: add tests on usages of value classes in Java.
    val klass = constructor.declarationDescriptor.toInlineClass() ?: return null
    if (!TypeUtils.isNullableType(this)) return klass

    val expandedUnderlyingType = unsubstitutedUnderlyingType() ?: return null
    if (!TypeUtils.isNullableType(expandedUnderlyingType) && !KotlinBuiltIns.isPrimitiveType(expandedUnderlyingType)) return klass

    return null
}

internal fun DeclarationDescriptor?.toInlineClass(): Class<*>? =
    if (this is ClassDescriptor && isInlineClass())
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
    val unboxMethod = expectedReceiverType?.toInlineClass()?.getInlineClassUnboxMethod(descriptor) ?: return this

    return unboxMethod.invoke(this)
}

private fun CallableDescriptor.isGetterOfUnderlyingPropertyOfValueClass() =
    this is PropertyGetterDescriptor && correspondingProperty.isUnderlyingPropertyOfValueClass()

private fun VariableDescriptor.isUnderlyingPropertyOfValueClass(): Boolean =
    extensionReceiverParameter == null && contextReceiverParameters.isEmpty() &&
            (containingDeclaration as? ClassDescriptor)?.valueClassRepresentation?.containsPropertyWithName(this.name) == true
