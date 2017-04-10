/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.eval4j.jdi

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

class JDIEval(
        private val vm: VirtualMachine,
        private val defaultClassLoader: ClassLoaderReference?,
        private val thread: ThreadReference,
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

    override fun loadClass(classType: Type): Value {
        return loadClass(classType, defaultClassLoader)
    }

    fun loadClass(classType: Type, classLoader: ClassLoaderReference?): Value {
        val loadedClasses = vm.classesByName(classType.internalName)
        if (!loadedClasses.isEmpty()) {
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
        }
        else {
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

    override fun loadString(str: String): Value = vm.mirrorOf(str).asValue()

    override fun newInstance(classType: Type): Value {
        return NewObjectValue(classType)
    }

    override fun isInstanceOf(value: Value, targetType: Type): Boolean {
        assert(targetType.sort == Type.OBJECT || targetType.sort == Type.ARRAY) {
            "Can't check isInstanceOf() for non-object type $targetType"
        }

        val _class = loadClass(targetType)
        return invokeMethod(
                _class,
                MethodDescription(
                        CLASS.internalName,
                        "isInstance",
                        "(Ljava/lang/Object;)Z",
                        false
                ),
                listOf(value)).boolean
    }

    fun Type.asReferenceType(classLoader: ClassLoaderReference? = this@JDIEval.defaultClassLoader): ReferenceType = loadClass(this, classLoader).jdiClass!!.reflectedType()
    fun Type.asArrayType(classLoader: ClassLoaderReference? = this@JDIEval.defaultClassLoader): ArrayType = asReferenceType(classLoader) as ArrayType

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
        if (!nestedSizes.isEmpty()) {
            val nestedElementType = elementType.arrayElementType
            val nestedSize = nestedSizes[0]
            val tail = nestedSizes.drop(1)
            for (i in 0..size - 1) {
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
        }
        catch (e: IndexOutOfBoundsException) {
            throwEvalException(ArrayIndexOutOfBoundsException(e.message))
        }
    }

    override fun setArrayElement(array: Value, index: Value, newValue: Value) {
        try {
            return array.array().setValue(index.int, newValue.asJdiValue(vm, array.asmType.arrayElementType))
        }
        catch (e: IndexOutOfBoundsException) {
            throwEvalException(ArrayIndexOutOfBoundsException(e.message))
        }
    }

    private fun findField(fieldDesc: FieldDescription): Field {
        val _class = fieldDesc.ownerType.asReferenceType()
        val field = _class.fieldByName(fieldDesc.name)
        if (field == null) {
            throwBrokenCodeException(NoSuchFieldError("Field not found: $fieldDesc"))
        }
        return field
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

        val _class = field.declaringType()
        if (_class !is ClassType) {
            throwBrokenCodeException(NoSuchFieldError("Can't a field in a non-class: $field"))
        }

        val jdiValue = newValue.asJdiValue(vm, field.type().asType())
        mayThrow { _class.setValue(field, jdiValue) }.ifFail(field)
    }

    private fun findMethod(methodDesc: MethodDescription, _class: ReferenceType = methodDesc.ownerType.asReferenceType()): Method {
        val methodName = methodDesc.name
        val method = when (_class) {
            is ClassType ->
                _class.concreteMethodByName(methodName, methodDesc.desc)
            else ->
                _class.methodsByName(methodName, methodDesc.desc).firstOrNull()
        }

        if (method != null) {
            return method
        }

        // Module name can be different for internal functions during evaluation and compilation
        val internalNameWithoutSuffix = internalNameWithoutModuleSuffix(methodName)
        if (internalNameWithoutSuffix != null) {
            val internalMethods = _class.visibleMethods().filter {
                val name = it.name()
                name.startsWith(internalNameWithoutSuffix) && canBeMangledInternalName(name) && it.signature() == methodDesc.desc
            }

            if (!internalMethods.isEmpty()) {
                return internalMethods.singleOrNull() ?:
                       throwBrokenCodeException(IllegalArgumentException("Several internal methods found for $methodDesc"))
            }
        }

        throwBrokenCodeException(NoSuchMethodError("Method not found: $methodDesc"))
    }

    override fun invokeStaticMethod(methodDesc: MethodDescription, arguments: List<Value>): Value {
        val method = findMethod(methodDesc)
        if (!method.isStatic) {
            throwBrokenCodeException(NoSuchMethodError("Method is not static: $methodDesc"))
        }
        val _class = method.declaringType()
        if (_class !is ClassType) throwBrokenCodeException(NoSuchMethodError("Static method is a non-class type: $method"))

        val args = mapArguments(arguments, method.safeArgumentTypes())

        if (shouldInvokeMethodWithReflection(method, args)) {
            return invokeMethodWithReflection(_class.asType(), NULL_VALUE, args, methodDesc)
        }

        args.disableCollection()
        val result = mayThrow { _class.invokeMethod(thread, method, args, invokePolicy) }.ifFail(method)
        args.enableCollection()
        return result.asValue()
    }

    override fun getField(instance: Value, fieldDesc: FieldDescription): Value {
        val field = findField(fieldDesc)
        val obj = instance.jdiObj.checkNull()

        return mayThrow { obj.getValue(field) }.ifFail(field, obj).asValue()
    }

    override fun setField(instance: Value, fieldDesc: FieldDescription, newValue: Value) {
        val field = findField(fieldDesc)
        val obj = instance.jdiObj.checkNull()

        val jdiValue = newValue.asJdiValue(vm, field.type().asType())
        mayThrow { obj.setValue(field, jdiValue) }
    }

    fun unboxType(boxedValue: Value, type: Type): Value {
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

    fun boxType(value: Value): Value {
        val method = when (value.asmType) {
            Type.INT_TYPE -> MethodDescription("java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false)
            Type.BYTE_TYPE -> MethodDescription("java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false)
            Type.SHORT_TYPE -> MethodDescription("java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false)
            Type.LONG_TYPE -> MethodDescription("java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false)
            Type.BOOLEAN_TYPE -> MethodDescription("java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false)
            Type.CHAR_TYPE -> MethodDescription("java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false)
            Type.FLOAT_TYPE -> MethodDescription("java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false)
            Type.DOUBLE_TYPE -> MethodDescription("java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false)
            else -> throw UnsupportedOperationException("Couldn't box non primitive type ${value.asmType.internalName}")
        }
        return invokeStaticMethod(method, listOf(value))
    }

    override fun invokeMethod(instance: Value, methodDesc: MethodDescription, arguments: List<Value>, invokespecial: Boolean): Value {
        if (invokespecial && methodDesc.name == "<init>") {
            // Constructor call
            val ctor = findMethod(methodDesc)
            val _class = (instance as NewObjectValue).asmType.asReferenceType() as ClassType
            val args = mapArguments(arguments, ctor.safeArgumentTypes())
            args.disableCollection()
            val result = mayThrow { _class.newInstance(thread, ctor, args, invokePolicy) }.ifFail(ctor)
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
            val result = mayThrow { obj.invokeMethod(thread, method, args, policy) }.ifFail(method, obj)
            args.enableCollection()
            return result.asValue()
        }

        val obj = instance.jdiObj.checkNull()
        if (invokespecial) {
            val method = findMethod(methodDesc)
            return doInvokeMethod(obj, method, invokePolicy or ObjectReference.INVOKE_NONVIRTUAL)
        }
        else {
            val method = findMethod(methodDesc, obj.referenceType() ?: methodDesc.ownerType.asReferenceType())
            return doInvokeMethod(obj, method, invokePolicy)
        }
    }

    private fun shouldInvokeMethodWithReflection(method: Method, args: List<com.sun.jdi.Value?>): Boolean {
        return !method.isVarArgs && args.zip(method.argumentTypes()).any { isArrayOfInterfaces(it.first?.type(), it.second) }
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
                listOf(instance, *args.map { it.asValue() }.toTypedArray())
        )

        if (methodDesc.returnType.sort != Type.OBJECT && methodDesc.returnType.sort != Type.ARRAY && methodDesc.returnType.sort != Type.VOID) {
            return unboxType(invocationResult, methodDesc.returnType)
        }
        return invocationResult
    }

    private fun List<jdi_Value?>.disableCollection() {
        forEach { (it as? ObjectReference)?.disableCollection() }
    }

    private fun List<jdi_Value?>.enableCollection() {
        forEach { (it as? ObjectReference)?.enableCollection() }
    }


    private fun mapArguments(arguments: List<Value>, expecetedTypes: List<jdi_Type>): List<jdi_Value?> {
        return arguments.zip(expecetedTypes).map {
            val (arg, expectedType) = it
            arg.asJdiValue(vm, expectedType.asType())
        }
    }

    private fun Method.safeArgumentTypes(): List<jdi_Type> {
        try {
            return argumentTypes()
        }
        catch (e: ClassNotLoadedException) {
            return argumentTypeNames()!!.map {
                name ->
                val dimensions = name.count { it == '[' }
                val baseTypeName = if (dimensions > 0) name.substring(0, name.indexOf('[')) else name

                val baseType = primitiveTypes[baseTypeName] ?: Type.getType("L$baseTypeName;").asReferenceType(declaringType().classLoader())

                if (dimensions == 0)
                    baseType
                else
                    Type.getType("[".repeat(dimensions) + baseType.asType().descriptor).asReferenceType(declaringType().classLoader())
            }
        }
    }
}

@Suppress("unused")
private sealed class JdiOperationResult<T> {
    class Fail<T>(val cause: Exception): JdiOperationResult<T>()
    class OK<T>(val value: T): JdiOperationResult<T>()
}

private fun <T> mayThrow(f: () -> T): JdiOperationResult<T> {
    try {
        return JdiOperationResult.OK(f())
    }
    catch (e: IllegalArgumentException) {
        return JdiOperationResult.Fail<T>(e)
    }
    catch (e: InvocationException) {
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
    return when(this) {
        is JdiOperationResult.OK -> this.value
        is JdiOperationResult.Fail -> {
            if (cause is IllegalArgumentException) {
                throwBrokenCodeException(IllegalArgumentException(lazyMessage(), this.cause))
            }
            else {
                throwBrokenCodeException(IllegalStateException(lazyMessage(), this.cause))
            }
        }
    }
}