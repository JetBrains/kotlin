/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal.calls

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.runtime.structure.desc
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.metadata.jvm.deserialization.ClassMapperLite
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.descriptorUtil.multiFieldValueClassRepresentation
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.asSimpleType
import org.jetbrains.kotlin.types.typeUtil.makeNullable
import java.lang.reflect.Member
import java.lang.reflect.Method
import java.lang.reflect.Type
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.reflect.jvm.internal.KDeclarationContainerImpl
import kotlin.reflect.jvm.internal.KotlinReflectionInternalError
import kotlin.reflect.jvm.internal.defaultPrimitiveValue
import kotlin.reflect.jvm.internal.toJavaClass

/**
 * A caller that is used whenever the declaration has value classes in its parameter types or inline class in return type.
 * Each argument of a value class type is unboxed, and the return value (if it's of an inline class type) is boxed.
 */
internal class ValueClassAwareCaller<out M : Member?>(
    descriptor: CallableMemberDescriptor,
    oldCaller: Caller<M>,
    private val isDefault: Boolean
) : Caller<M> {

    private val caller: Caller<M> = if (oldCaller is CallerImpl.Method.BoundStatic) {
        val receiverType = (descriptor.extensionReceiverParameter ?: descriptor.dispatchReceiverParameter)?.type
        if (
            receiverType != null &&
            receiverType.needsMfvcFlattening() &&
            (!isDefault || descriptor.valueParameters.any { it.declaresDefaultValue() })
        ) {
            val unboxMethods = getMfvcUnboxMethods(receiverType.asSimpleType())!!
            val boundReceiverComponents = unboxMethods.map { it.invoke(oldCaller.boundReceiver) }.toTypedArray()
            @Suppress("UNCHECKED_CAST")
            CallerImpl.Method.BoundStaticMultiFieldValueClass(oldCaller.member, boundReceiverComponents) as Caller<M>
        } else {
            oldCaller
        }
    } else {
        oldCaller
    }

    override val member: M = caller.member


    override val returnType: Type
        get() = caller.returnType

    override val parameterTypes: List<Type>
        get() = caller.parameterTypes

    private class BoxUnboxData(val argumentRange: IntRange, val unboxParameters: Array<List<Method>?>, val box: Method?)

    private val data: BoxUnboxData = run {
        val returnType = descriptor.returnType!!
        val box = if (
            descriptor is FunctionDescriptor && descriptor.isSuspend &&
            returnType.substitutedUnderlyingType()?.let { KotlinBuiltIns.isPrimitiveType(it) } == true
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
            caller is CallerImpl.Method.BoundStatic || caller is CallerImpl.Method.BoundStaticMultiFieldValueClass -> {
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

        val flattenedShift = if (caller is CallerImpl.Method.BoundStaticMultiFieldValueClass) -caller.receiverComponentsCount else shift

        val kotlinParameterTypes: List<KotlinType> = makeKotlinParameterTypes(descriptor, caller.member) { isValueClass() }

        fun typeSize(type: KotlinType): Int = getMfvcUnboxMethods(type.asSimpleType())?.size ?: 1

        // If the default argument is set,
        // (kotlinParameterTypes.size + Int.SIZE_BITS - 1) / Int.SIZE_BITS masks and one marker are added to the end of the argument.
        val extraArgumentsTail =
            (if (isDefault) ((kotlinParameterTypes.sumOf(::typeSize) + Int.SIZE_BITS - 1) / Int.SIZE_BITS) + 1 else 0) +
                    (if (descriptor is FunctionDescriptor && descriptor.isSuspend) 1 else 0)
        val expectedArgsSize = kotlinParameterTypes.sumOf(::typeSize) + flattenedShift + extraArgumentsTail
        checkParametersSize(expectedArgsSize, descriptor, isDefault)

        // maxOf is needed because in case of a bound top level extension, shift can be -1 (see above). But in that case, we need not unbox
        // the extension receiver argument, since it has already been unboxed at compile time and generated into the reference
        val argumentRange = maxOf(shift, 0) until (kotlinParameterTypes.size + shift)

        val unbox = Array(expectedArgsSize) { i ->
            if (i in argumentRange)
                getValueClassUnboxMethods(kotlinParameterTypes[i - shift].asSimpleType(), descriptor)
            else null
        }

        BoxUnboxData(argumentRange, unbox, box)
    }

    private val slices = buildList {
        var currentOffset = when (caller) {
            is CallerImpl.Method.BoundStaticMultiFieldValueClass -> caller.boundReceiverComponents.size
            is CallerImpl.Method.BoundStatic -> 1
            else -> 0
        }
        if (currentOffset > 0) {
            add(0 until currentOffset)
        }
        for (parameterUnboxMethods in data.unboxParameters) {
            val length = parameterUnboxMethods?.size ?: 1
            add(currentOffset until (currentOffset + length))
            currentOffset += length
        }
    }.toTypedArray()

    fun getRealSlicesOfParameters(index: Int): IntRange = when {
        index in slices.indices -> slices[index]
        slices.isEmpty() -> index..index
        else -> {
            val start = (index - slices.size) + (slices.last().last + 1)
            start..start
        }
    }

    private val hasMfvcParameters = data.argumentRange.any { (data.unboxParameters[it] ?: return@any false).size > 1 }

    override fun call(args: Array<*>): Any? {
        val range = data.argumentRange
        val unbox = data.unboxParameters
        val box = data.box

        val unboxedArguments = when {
            range.isEmpty() -> args
            hasMfvcParameters -> buildList(args.size) {
                for (index in 0 until range.first) {
                    add(args[index])
                }
                for (index in range) {
                    val methods = unbox[index]
                    val arg = args[index]
                    // Note that arg may be null in case we're calling a $default method, and it's an optional parameter of a value class type
                    if (methods != null) {
                        methods.mapTo(this) { if (arg != null) it.invoke(arg) else defaultPrimitiveValue(it.returnType) }
                    } else {
                        add(arg)
                    }
                }
                for (index in (range.last + 1)..args.lastIndex) {
                    add(args[index])
                }
            }.toTypedArray()
            else -> Array(args.size) { index ->
                if (index in range) {
                    val method = unbox[index]?.single()
                    val arg = args[index]
                    // Note that arg may be null in case we're calling a $default method, and it's an optional parameter of an inline class type
                    when {
                        method == null -> arg
                        arg != null -> method.invoke(arg)
                        else -> defaultPrimitiveValue(method.returnType)
                    }
                } else {
                    args[index]
                }
            }
        }

        val result = caller.call(unboxedArguments)
        if (result === COROUTINE_SUSPENDED) return result

        // box is not null only for inline classes
        return box?.invoke(null, result) ?: result
    }

    class MultiFieldValueClassPrimaryConstructorCaller(
        descriptor: FunctionDescriptor,
        container: KDeclarationContainerImpl,
        constructorDesc: String,
        originalParameters: List<ParameterDescriptor>
    ) : Caller<Nothing?> {
        private val constructorImpl = container.findMethodBySignature("constructor-impl", constructorDesc)!!
        private val boxMethod = container.findMethodBySignature("box-impl", constructorDesc.removeSuffix("V") + container.jClass.desc)!!
        private val parameterUnboxMethods: List<List<Method>?> = originalParameters.map { parameter ->
            getValueClassUnboxMethods(parameter.type.asSimpleType(), descriptor)
        }
        override val member: Nothing?
            get() = null
        override val returnType: Type
            get() = boxMethod.returnType

        val originalParametersGroups: List<List<Class<*>>> = originalParameters.mapIndexed { index, it ->
            val classDescriptor = it.type.constructor.declarationDescriptor as ClassDescriptor
            parameterUnboxMethods[index]?.map { it.returnType } ?: listOf(classDescriptor.toJavaClass()!!)
        }

        override val parameterTypes: List<Type> = originalParametersGroups.flatten()

        override fun call(args: Array<*>): Any? {
            val newArgs = (args zip parameterUnboxMethods)
                .flatMap { (arg, unboxMethods) -> unboxMethods?.map { it.invoke(arg) } ?: listOf(arg) }.toTypedArray()
            constructorImpl.invoke(null, *newArgs)
            return boxMethod.invoke(null, *newArgs)
        }
    }
}

internal fun ClassifierDescriptor.toJvmDescriptor(): String = ClassMapperLite.mapClass(classId!!.asString())


private fun getValueClassUnboxMethods(type: SimpleType, descriptor: CallableMemberDescriptor): List<Method>? =
    getMfvcUnboxMethods(type) ?: type.toInlineClass()?.getInlineClassUnboxMethod(descriptor)?.let(::listOf)

internal fun getMfvcUnboxMethods(type: SimpleType): List<Method>? {
    fun getUnboxMethodNameSuffixes(type: SimpleType): List<String>? =
        if (type.needsMfvcFlattening()) (type.constructor.declarationDescriptor as ClassDescriptor)
            .multiFieldValueClassRepresentation!!.underlyingPropertyNamesToTypes.flatMap { (name, innerType) ->
                getUnboxMethodNameSuffixes(innerType)?.map { "${name.identifier}-${it}" } ?: listOf(name.identifier)
            }
        else null

    val unboxMethodsNames = getUnboxMethodNameSuffixes(type.asSimpleType())?.map { "unbox-impl-$it" } ?: return null
    val javaClass = (type.constructor.declarationDescriptor as ClassDescriptor).toJavaClass()!!
    return unboxMethodsNames.map { javaClass.getDeclaredMethod(it) }
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

private fun makeKotlinParameterTypes(
    descriptor: CallableMemberDescriptor, member: Member?, isSpecificClass: ClassDescriptor.() -> Boolean
): List<KotlinType> = ArrayList<KotlinType>().also { kotlinParameterTypes ->
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
        if (containingDeclaration is ClassDescriptor && containingDeclaration.isSpecificClass()) {
            if (member?.acceptsBoxedReceiverParameter() == true) {
                // hack to forbid unboxing dispatchReceiver if it is used upcasted
                // kotlinParameterTypes are used to determine shifts and calls according to whether type is MFVC/IC or not.
                // If it is a MFVC/IC, boxes are unboxed. If the actual called member lies in the interface/DefaultImpls class,
                // it accepts a boxed parameter as ex-dispatch receiver. Making the type nullable allows to prevent unboxing in this case.
                kotlinParameterTypes.add(containingDeclaration.defaultType.makeNullable())
            } else {
                kotlinParameterTypes.add(containingDeclaration.defaultType)
            }
        }
    }

    descriptor.valueParameters.mapTo(kotlinParameterTypes, ValueParameterDescriptor::getType)
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
    isDefault: Boolean = false
): Caller<M> {
    val needsValueClassAwareCaller: Boolean =
        descriptor.isGetterOfUnderlyingPropertyOfValueClass() ||
                descriptor.contextReceiverParameters.any { it.type.isValueClassType() } ||
                descriptor.valueParameters.any { it.type.isValueClassType() } ||
                descriptor.returnType?.isInlineClassType() == true ||
                descriptor.hasValueClassReceiver()

    return if (needsValueClassAwareCaller) ValueClassAwareCaller(descriptor, this, isDefault) else this
}

private fun CallableMemberDescriptor.hasValueClassReceiver() =
    expectedReceiverType?.isValueClassType() == true

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
