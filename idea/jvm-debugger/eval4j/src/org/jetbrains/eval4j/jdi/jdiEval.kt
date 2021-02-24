/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.eval4j.jdi

import com.intellij.openapi.util.text.StringUtil
import com.sun.jdi.*
import org.jetbrains.eval4j.*
import org.jetbrains.eval4j.Value
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper.InternalNameMapper.canBeMangledInternalName
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper.InternalNameMapper.internalNameWithoutModuleSuffix
import org.jetbrains.org.objectweb.asm.Type
import java.lang.reflect.AccessibleObject
import com.sun.jdi.Type as jdi_Type
import com.sun.jdi.Value as jdi_Value

private val CLASS = Type.getType(Class::class.java)
private val OBJECT = Type.getType(Any::class.java)
private val BOOTSTRAP_CLASS_DESCRIPTORS = setOf("Ljava/lang/String;", "Ljava/lang/ClassLoader;", "Ljava/lang/Class;")

open class JDIEval(
    private val vm: VirtualMachine,
    private val defaultClassLoader: ClassLoaderReference?,
    protected val thread: ThreadReference,
    private val invokePolicy: Int
) : Eval {

    private val primitiveTypes = mapOf(
        Type.BOOLEAN_TYPE.className to vm.mirrorOf(true).type(),
        Type.BYTE_TYPE.className to vm.mirrorOf(1.toByte()).type(),
        Type.SHORT_TYPE.className to vm.mirrorOf(1.toShort()).type(),
        Type.INT_TYPE.className to vm.mirrorOf(1).type(),
        Type.CHAR_TYPE.className to vm.mirrorOf('1').type(),
        Type.LONG_TYPE.className to vm.mirrorOf(1L).type(),
        Type.FLOAT_TYPE.className to vm.mirrorOf(1.0f).type(),
        Type.DOUBLE_TYPE.className to vm.mirrorOf(1.0).type()
    )

    private val isJava8OrLater = StringUtil.compareVersionNumbers(vm.version(), "1.8") >= 0

    override fun loadClass(classType: Type): Value {
        return loadClass(classType, defaultClassLoader)
    }

    private fun loadClass(classType: Type, classLoader: ClassLoaderReference?): Value {
        val loadedClasses = vm.classesByName(classType.internalName)
        if (loadedClasses.isNotEmpty()) {
            for (loadedClass in loadedClasses) {
                if (loadedClass.isPrepared && (classType.descriptor in BOOTSTRAP_CLASS_DESCRIPTORS || loadedClass.classLoader() == classLoader)) {
                    return loadedClass.classObject().asValue()
                }
            }
        }
        if (classLoader == null) {
            return invokeStaticMethod(
                MethodDescription(
                    CLASS.internalName,
                    "forName",
                    "(Ljava/lang/String;)Ljava/lang/Class;",
                    true
                ),
                listOf(vm.mirrorOf(classType.internalName.replace('/', '.')).asValue())
            )
        } else {
            return invokeStaticMethod(
                MethodDescription(
                    CLASS.internalName,
                    "forName",
                    "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;",
                    true
                ),
                listOf(
                    vm.mirrorOf(classType.internalName.replace('/', '.')).asValue(),
                    boolean(true),
                    classLoader.asValue()
                )
            )
        }
    }

    private fun loadClassByName(name: String, classLoader: ClassLoaderReference): jdi_Type {
        val dimensions = name.count { it == '[' }
        val baseTypeName = if (dimensions > 0) name.substring(0, name.indexOf('[')) else name

        val baseType = primitiveTypes[baseTypeName] ?: Type.getType("L$baseTypeName;").asReferenceType(classLoader)

        return if (dimensions == 0)
            baseType
        else
            Type.getType("[".repeat(dimensions) + baseType.asType().descriptor).asReferenceType(classLoader)
    }

    override fun loadString(str: String): Value = vm.mirrorOf(str).asValue()

    override fun newInstance(classType: Type): Value {
        return NewObjectValue(classType)
    }

    override fun isInstanceOf(value: Value, targetType: Type): Boolean {
        assert(targetType.sort == Type.OBJECT || targetType.sort == Type.ARRAY) {
            "Can't check isInstanceOf() for non-object type $targetType"
        }

        val clazz = loadClass(targetType)
        return invokeMethod(
            clazz,
            MethodDescription(
                CLASS.internalName,
                "isInstance",
                "(Ljava/lang/Object;)Z",
                false
            ),
            listOf(value)
        ).boolean
    }

    private fun Type.asReferenceType(classLoader: ClassLoaderReference? = this@JDIEval.defaultClassLoader): ReferenceType =
        loadClass(this, classLoader).jdiClass!!.reflectedType()

    private fun Type.asArrayType(classLoader: ClassLoaderReference? = this@JDIEval.defaultClassLoader): ArrayType =
        asReferenceType(classLoader) as ArrayType

    override fun newArray(arrayType: Type, size: Int): Value {
        val jdiArrayType = arrayType.asArrayType()
        return jdiArrayType.newInstance(size).asValue()
    }

    private val Type.arrayElementType: Type
        get(): Type {
            assert(sort == Type.ARRAY) { "Not an array type: $this" }
            return Type.getType(descriptor.substring(1))
        }

    private fun fillArray(elementType: Type, size: Int, nestedSizes: List<Int>): Value {
        val arr = newArray(Type.getType("[" + elementType.descriptor), size)
        if (nestedSizes.isNotEmpty()) {
            val nestedElementType = elementType.arrayElementType
            val nestedSize = nestedSizes[0]
            val tail = nestedSizes.drop(1)
            for (i in 0 until size) {
                setArrayElement(arr, int(i), fillArray(nestedElementType, nestedSize, tail))
            }
        }
        return arr
    }

    override fun newMultiDimensionalArray(arrayType: Type, dimensionSizes: List<Int>): Value {
        return fillArray(arrayType.arrayElementType, dimensionSizes[0], dimensionSizes.drop(1))
    }

    private fun Value.array() = jdiObj.checkNull() as ArrayReference

    override fun getArrayLength(array: Value): Value {
        return int(array.array().length())
    }

    override fun getArrayElement(array: Value, index: Value): Value {
        try {
            return array.array().getValue(index.int).asValue()
        } catch (e: IndexOutOfBoundsException) {
            throwInterpretingException(ArrayIndexOutOfBoundsException(e.message))
        }
    }

    override fun setArrayElement(array: Value, index: Value, newValue: Value) {
        try {
            return array.array().setValue(index.int, newValue.asJdiValue(vm, array.asmType.arrayElementType))
        } catch (e: IndexOutOfBoundsException) {
            throwInterpretingException(ArrayIndexOutOfBoundsException(e.message))
        }
    }

    private fun findField(fieldDesc: FieldDescription, receiver: ReferenceType? = null): Field {
        receiver?.fieldByName(fieldDesc.name)?.let { return it }
        fieldDesc.ownerType.asReferenceType().fieldByName(fieldDesc.name)?.let { return it }

        throwBrokenCodeException(NoSuchFieldError("Field not found: $fieldDesc"))
    }

    private fun findStaticField(fieldDesc: FieldDescription): Field {
        val field = findField(fieldDesc)
        if (!field.isStatic) {
            throwBrokenCodeException(NoSuchFieldError("Field is not static: $fieldDesc"))
        }
        return field
    }

    override fun getStaticField(fieldDesc: FieldDescription): Value {
        val field = findStaticField(fieldDesc)
        return mayThrow { field.declaringType().getValue(field) }.ifFail(field).asValue()
    }

    override fun setStaticField(fieldDesc: FieldDescription, newValue: Value) {
        val field = findStaticField(fieldDesc)

        if (field.isFinal) {
            throwBrokenCodeException(NoSuchFieldError("Can't modify a final field: $field"))
        }

        val clazz = field.declaringType() as? ClassType
            ?: throwBrokenCodeException(NoSuchFieldError("Can't a field in a non-class: $field"))

        val jdiValue = newValue.asJdiValue(vm, field.type().asType())
        mayThrow { clazz.setValue(field, jdiValue) }.ifFail(field)
    }

    private fun findMethod(methodDesc: MethodDescription, clazz: ReferenceType = methodDesc.ownerType.asReferenceType()): Method {
        val method = findMethodOrNull(methodDesc, clazz)
        if (method != null) {
            return method
        }

        throwBrokenCodeException(NoSuchMethodError("Method not found: $methodDesc"))
    }

    private fun findMethodOrNull(methodDesc: MethodDescription, clazz: ReferenceType): Method? {
        val methodName = methodDesc.name

        var method: Method?
        when (clazz) {
            is ClassType -> {
                method = clazz.concreteMethodByName(methodName, methodDesc.desc)
            }
            is ArrayType -> { // Copied from com.intellij.debugger.engine.DebuggerUtils.findMethod
                val objectType = OBJECT.asReferenceType()
                method = findMethodOrNull(methodDesc, objectType)
                if (method == null && methodDesc.name == "clone" && methodDesc.desc == "()[Ljava/lang/Object;") {
                    method = findMethodOrNull(MethodDescription(OBJECT.internalName, "clone", "()[Ljava/lang/Object;", false), objectType)
                }
            }
            else -> {
                method = clazz.methodsByName(methodName, methodDesc.desc).firstOrNull()
            }
        }

        if (method != null) {
            return method
        }

        // Module name can be different for internal functions during evaluation and compilation
        val internalNameWithoutSuffix = internalNameWithoutModuleSuffix(methodName)
        if (internalNameWithoutSuffix != null) {
            val internalMethods = clazz.visibleMethods().filter {
                val name = it.name()
                name.startsWith(internalNameWithoutSuffix) && canBeMangledInternalName(name) && it.signature() == methodDesc.desc
            }

            if (internalMethods.isNotEmpty()) {
                return internalMethods.singleOrNull()
                    ?: throwBrokenCodeException(IllegalArgumentException("Several internal methods found for $methodDesc"))
            }
        }

        return null
    }

    open fun jdiInvokeStaticMethod(type: ClassType, method: Method, args: List<jdi_Value?>, invokePolicy: Int): jdi_Value? {
        return type.invokeMethod(thread, method, args, invokePolicy)
    }

    open fun jdiInvokeStaticMethod(type: InterfaceType, method: Method, args: List<jdi_Value?>, invokePolicy: Int): jdi_Value? {
        return type.invokeMethod(thread, method, args, invokePolicy)
    }

    override fun invokeStaticMethod(methodDesc: MethodDescription, arguments: List<Value>): Value {
        val method = findMethod(methodDesc)
        if (!method.isStatic) {
            throwBrokenCodeException(NoSuchMethodError("Method is not static: $methodDesc"))
        }
        val declaringType = method.declaringType()
        val args = mapArguments(arguments, method.safeArgumentTypes())

        if (shouldInvokeMethodWithReflection(method, args)) {
            return invokeMethodWithReflection(declaringType.asType(), NULL_VALUE, args, methodDesc)
        }

        args.disableCollection()

        val result = mayThrow {
            when (declaringType) {
                is ClassType -> jdiInvokeStaticMethod(declaringType, method, args, invokePolicy)
                is InterfaceType -> {
                    if (!isJava8OrLater) {
                        val message = "Calling interface static methods is not supported in JVM ${vm.version()} ($method)"
                        throwBrokenCodeException(NoSuchMethodError(message))
                    }

                    jdiInvokeStaticMethod(declaringType, method, args, invokePolicy)
                }
                else -> {
                    val message = "Calling static methods is only supported for classes and interfaces ($method)"
                    throwBrokenCodeException(NoSuchMethodError(message))
                }
            }
        }.ifFail(method)

        args.enableCollection()
        return result.asValue()
    }

    override fun getField(instance: Value, fieldDesc: FieldDescription): Value {
        val receiver = instance.jdiObj.checkNull()
        val field = findField(fieldDesc, receiver.referenceType())

        return mayThrow {
            try {
                receiver.getValue(field)
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException(
                    "Possibly incompatible types: " +
                            "field declaring type = ${field.declaringType()}, " +
                            "instance type = ${receiver.referenceType()}"
                )
            }
        }.ifFail(field, receiver).asValue()
    }

    override fun setField(instance: Value, fieldDesc: FieldDescription, newValue: Value) {
        val receiver = instance.jdiObj.checkNull()
        val field = findField(fieldDesc, receiver.referenceType())

        val jdiValue = newValue.asJdiValue(vm, field.type().asType())
        mayThrow { receiver.setValue(field, jdiValue) }
    }

    private fun unboxType(boxedValue: Value, type: Type): Value {
        val method = when (type) {
            Type.INT_TYPE -> MethodDescription("java/lang/Integer", "intValue", "()I", false)
            Type.BOOLEAN_TYPE -> MethodDescription("java/lang/Boolean", "booleanValue", "()Z", false)
            Type.CHAR_TYPE -> MethodDescription("java/lang/Character", "charValue", "()C", false)
            Type.SHORT_TYPE -> MethodDescription("java/lang/Character", "shortValue", "()S", false)
            Type.LONG_TYPE -> MethodDescription("java/lang/Long", "longValue", "()J", false)
            Type.BYTE_TYPE -> MethodDescription("java/lang/Byte", "byteValue", "()B", false)
            Type.FLOAT_TYPE -> MethodDescription("java/lang/Float", "floatValue", "()F", false)
            Type.DOUBLE_TYPE -> MethodDescription("java/lang/Double", "doubleValue", "()D", false)
            else -> throw UnsupportedOperationException("Couldn't unbox non primitive type ${type.internalName}")
        }
        return invokeMethod(boxedValue, method, listOf(), true)
    }

    open fun jdiInvokeMethod(obj: ObjectReference, method: Method, args: List<jdi_Value?>, policy: Int): jdi_Value? {
        return obj.invokeMethod(thread, method, args, policy)
    }

    override fun invokeMethod(instance: Value, methodDesc: MethodDescription, arguments: List<Value>, invokespecial: Boolean): Value {
        if (invokespecial && methodDesc.name == "<init>") {
            // Constructor call
            val ctor = findMethod(methodDesc)
            val clazz = (instance as NewObjectValue).asmType.asReferenceType() as ClassType
            val args = mapArguments(arguments, ctor.safeArgumentTypes())
            args.disableCollection()
            val result = mayThrow { clazz.newInstance(thread, ctor, args, invokePolicy) }.ifFail(ctor)
            args.enableCollection()
            instance.value = result
            return result.asValue()
        }

        fun doInvokeMethod(obj: ObjectReference, method: Method, policy: Int): Value {
            val args = mapArguments(arguments, method.safeArgumentTypes())

            if (shouldInvokeMethodWithReflection(method, args)) {
                return invokeMethodWithReflection(instance.asmType, instance, args, methodDesc)
            }

            args.disableCollection()
            val result = mayThrow { jdiInvokeMethod(obj, method, args, policy) }.ifFail(method, obj)
            args.enableCollection()
            return result.asValue()
        }

        val obj = instance.jdiObj.checkNull()
        return if (invokespecial) {
            val method = findMethod(methodDesc)
            doInvokeMethod(obj, method, invokePolicy or ObjectReference.INVOKE_NONVIRTUAL)
        } else {
            val method = findMethod(methodDesc, obj.referenceType() ?: methodDesc.ownerType.asReferenceType())
            doInvokeMethod(obj, method, invokePolicy)
        }
    }

    private fun shouldInvokeMethodWithReflection(method: Method, args: List<com.sun.jdi.Value?>): Boolean {
        if (method.isVarArgs) {
            return false
        }

        val argumentTypes = try {
            method.argumentTypes()
        } catch (e: ClassNotLoadedException) {
            return false
        }

        return args.zip(argumentTypes).any { isArrayOfInterfaces(it.first?.type(), it.second) }
    }

    private fun isArrayOfInterfaces(valueType: jdi_Type?, expectedType: jdi_Type?): Boolean {
        return (valueType as? ArrayType)?.componentType() is InterfaceType && (expectedType as? ArrayType)?.componentType() == OBJECT.asReferenceType()
    }

    private fun invokeMethodWithReflection(ownerType: Type, instance: Value, args: List<jdi_Value?>, methodDesc: MethodDescription): Value {
        val methodToInvoke = invokeMethod(
            loadClass(ownerType),
            MethodDescription(
                CLASS.internalName,
                "getDeclaredMethod",
                "(Ljava/lang/String;[L${CLASS.internalName};)Ljava/lang/reflect/Method;",
                true
            ),
            listOf(vm.mirrorOf(methodDesc.name).asValue(), *methodDesc.parameterTypes.map { loadClass(it) }.toTypedArray())
        )

        invokeMethod(
            methodToInvoke,
            MethodDescription(
                Type.getType(AccessibleObject::class.java).internalName,
                "setAccessible",
                "(Z)V",
                true
            ),
            listOf(vm.mirrorOf(true).asValue())
        )

        val invocationResult = invokeMethod(
            methodToInvoke,
            MethodDescription(
                methodToInvoke.asmType.internalName,
                "invoke",
                "(L${OBJECT.internalName};[L${OBJECT.internalName};)L${OBJECT.internalName};",
                true
            ),
            listOf(instance, mirrorOfArgs(args))
        )

        return if (methodDesc.returnType.sort != Type.OBJECT &&
            methodDesc.returnType.sort != Type.ARRAY &&
            methodDesc.returnType.sort != Type.VOID
        )
            unboxType(invocationResult, methodDesc.returnType)
        else
            invocationResult
    }

    private fun mirrorOfArgs(args: List<jdi_Value?>): Value {
        val arrayObject = newArray(Type.getType("[" + OBJECT.descriptor), args.size)

        args.forEachIndexed { index, value ->
            val indexValue = vm.mirrorOf(index).asValue()
            setArrayElement(arrayObject, indexValue, value.asValue())
        }

        return arrayObject
    }

    private fun List<jdi_Value?>.disableCollection() {
        forEach { (it as? ObjectReference)?.disableCollection() }
    }

    private fun List<jdi_Value?>.enableCollection() {
        forEach { (it as? ObjectReference)?.enableCollection() }
    }


    private fun mapArguments(arguments: List<Value>, expectedTypes: List<jdi_Type>): List<jdi_Value?> {
        return arguments.zip(expectedTypes).map {
            val (arg, expectedType) = it
            arg.asJdiValue(vm, expectedType.asType())
        }
    }

    private fun Method.safeArgumentTypes(): List<jdi_Type> {
        try {
            return argumentTypes()
        } catch (e: ClassNotLoadedException) {
            return argumentTypeNames()!!.map { name ->
                val classLoader = declaringType()?.classLoader()
                if (classLoader != null) {
                    return@map loadClassByName(name, classLoader)
                }

                when (name) {
                    "void" -> virtualMachine().mirrorOfVoid().type()
                    "boolean" -> primitiveTypes.getValue(Type.BOOLEAN_TYPE.className)
                    "byte" -> primitiveTypes.getValue(Type.BYTE_TYPE.className)
                    "char" -> primitiveTypes.getValue(Type.CHAR_TYPE.className)
                    "short" -> primitiveTypes.getValue(Type.SHORT_TYPE.className)
                    "int" -> primitiveTypes.getValue(Type.INT_TYPE.className)
                    "long" -> primitiveTypes.getValue(Type.LONG_TYPE.className)
                    "float" -> primitiveTypes.getValue(Type.FLOAT_TYPE.className)
                    "double" -> primitiveTypes.getValue(Type.DOUBLE_TYPE.className)
                    else -> virtualMachine().classesByName(name).firstOrNull()
                        ?: throw IllegalStateException("Unknown class $name")
                }
            }
        }
    }
}

@Suppress("unused")
private sealed class JdiOperationResult<T> {
    class Fail<T>(val cause: Exception) : JdiOperationResult<T>()
    class OK<T>(val value: T) : JdiOperationResult<T>()
}

private fun <T> mayThrow(f: () -> T): JdiOperationResult<T> {
    return try {
        JdiOperationResult.OK(f())
    } catch (e: IllegalArgumentException) {
        JdiOperationResult.Fail(e)
    } catch (e: InvocationException) {
        throw ThrownFromEvaluatedCodeException(e.exception().asValue())
    }
}

private fun memberInfo(member: TypeComponent, thisObj: ObjectReference?): String {
    return "\nmember = $member\nobjectRef = $thisObj"
}

private fun <T> JdiOperationResult<T>.ifFail(member: TypeComponent, thisObj: ObjectReference? = null): T {
    return ifFail { memberInfo(member, thisObj) }
}

private fun <T> JdiOperationResult<T>.ifFail(lazyMessage: () -> String): T {
    return when (this) {
        is JdiOperationResult.OK -> this.value
        is JdiOperationResult.Fail -> {
            if (cause is IllegalArgumentException) {
                throwBrokenCodeException(IllegalArgumentException(lazyMessage(), this.cause))
            } else {
                throwBrokenCodeException(IllegalStateException(lazyMessage(), this.cause))
            }
        }
    }
}