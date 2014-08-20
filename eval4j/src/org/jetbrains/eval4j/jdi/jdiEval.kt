/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

import org.jetbrains.eval4j.*
import org.jetbrains.org.objectweb.asm.Type
import com.sun.jdi
import com.sun.jdi.ClassNotLoadedException
import com.sun.tools.jdi.ReferenceTypeImpl
import com.sun.jdi.ObjectReference
import com.sun.jdi.Method

val CLASS = Type.getType(javaClass<Class<*>>())
val BOOTSTRAP_CLASS_DESCRIPTORS = setOf("Ljava/lang/String;", "Ljava/lang/ClassLoader;", "Ljava/lang/Class;")

public class JDIEval(
        private val vm: jdi.VirtualMachine,
        private val classLoader: jdi.ClassLoaderReference,
        private val thread: jdi.ThreadReference,
        private val invokePolicy: Int
) : Eval {

    private val primitiveTypes = mapOf(
            Type.BOOLEAN_TYPE.getClassName() to vm.mirrorOf(true).`type`(),
            Type.BYTE_TYPE.getClassName() to vm.mirrorOf(1.toByte()).`type`(),
            Type.SHORT_TYPE.getClassName() to vm.mirrorOf(1.toShort()).`type`(),
            Type.INT_TYPE.getClassName() to vm.mirrorOf(1.toInt()).`type`(),
            Type.CHAR_TYPE.getClassName() to vm.mirrorOf('1').`type`(),
            Type.LONG_TYPE.getClassName() to vm.mirrorOf(1L).`type`(),
            Type.FLOAT_TYPE.getClassName() to vm.mirrorOf(1.0f).`type`(),
            Type.DOUBLE_TYPE.getClassName() to vm.mirrorOf(1.0).`type`()
    )

    override fun loadClass(classType: Type): Value {
        val loadedClasses = vm.classesByName(classType.getInternalName())
        if (!loadedClasses.isEmpty()) {
            val loadedClass = loadedClasses[0]
            if (classType.getDescriptor() in BOOTSTRAP_CLASS_DESCRIPTORS || loadedClass.classLoader() == classLoader) {
                return loadedClass.classObject().asValue()
            }
        }
        return invokeStaticMethod(
                MethodDescription(
                        CLASS.getInternalName(),
                        "forName",
                        "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;",
                        true
                ),
                listOf(
                        vm.mirrorOf(classType.getInternalName().replace('/', '.')).asValue(),
                        boolean(true),
                        classLoader.asValue()
                )
        )
    }

    override fun loadString(str: String): Value = vm.mirrorOf(str).asValue()

    override fun newInstance(classType: Type): Value {
        return NewObjectValue(classType)
    }

    override fun isInstanceOf(value: Value, targetType: Type): Boolean {
        assert(targetType.getSort() == Type.OBJECT || targetType.getSort() == Type.ARRAY, "Can't check isInstanceOf() for non-object type $targetType")

        val _class = loadClass(targetType)
        return invokeMethod(
                _class,
                MethodDescription(
                        CLASS.getInternalName(),
                        "isInstance",
                        "(Ljava/lang/Object;)Z",
                        false
                ),
                listOf(value)).boolean
    }

    fun Type.asReferenceType(): jdi.ReferenceType = loadClass(this).jdiClass!!.reflectedType()
    fun Type.asArrayType(): jdi.ArrayType = asReferenceType() as jdi.ArrayType

    override fun newArray(arrayType: Type, size: Int): Value {
        val jdiArrayType = arrayType.asArrayType()
        return jdiArrayType.newInstance(size).asValue()
    }

    private val Type.arrayElementType: Type
        get(): Type {
            assert(getSort() == Type.ARRAY, "Not an array type: $this")
            return Type.getType(getDescriptor().substring(1))
        }

    private fun fillArray(elementType: Type, size: Int, nestedSizes: List<Int>): Value {
        val arr = newArray(Type.getType("[" + elementType.getDescriptor()), size)
        if (!nestedSizes.isEmpty()) {
            val nestedElementType = elementType.arrayElementType
            val nestedSize = nestedSizes[0]
            val tail = nestedSizes.tail
            for (i in 0..size - 1) {
                setArrayElement(arr, int(i), fillArray(nestedElementType, nestedSize, tail))
            }
        }
        return arr
    }

    override fun newMultiDimensionalArray(arrayType: Type, dimensionSizes: List<Int>): Value {
        return fillArray(arrayType.arrayElementType, dimensionSizes[0], dimensionSizes.tail)
    }

    private fun Value.array() = jdiObj.checkNull() as jdi.ArrayReference

    override fun getArrayLength(array: Value): Value {
        return int(array.array().length())
    }

    override fun getArrayElement(array: Value, index: Value): Value {
        try {
            return array.array().getValue(index.int).asValue()
        }
        catch (e: IndexOutOfBoundsException) {
            throwEvalException(ArrayIndexOutOfBoundsException(e.getMessage()))
        }
    }

    override fun setArrayElement(array: Value, index: Value, newValue: Value) {
        try {
            return array.array().setValue(index.int, newValue.asJdiValue(vm, array.asmType.arrayElementType))
        }
        catch (e: IndexOutOfBoundsException) {
            throwEvalException(ArrayIndexOutOfBoundsException(e.getMessage()))
        }
    }

    private fun findField(fieldDesc: FieldDescription): jdi.Field {
        val _class = fieldDesc.ownerType.asReferenceType()
        val field = _class.fieldByName(fieldDesc.name)
        if (field == null) {
            throwBrokenCodeException(NoSuchFieldError("Field not found: $fieldDesc"))
        }
        return field
    }

    private fun findStaticField(fieldDesc: FieldDescription): jdi.Field {
        val field = findField(fieldDesc)
        if (!field.isStatic()) {
            throwBrokenCodeException(NoSuchFieldError("Field is not static: $fieldDesc"))
        }
        return field
    }

    override fun getStaticField(fieldDesc: FieldDescription): Value {
        val field = findStaticField(fieldDesc)
        return mayThrow { field.declaringType().getValue(field) }.asValue()
    }

    override fun setStaticField(fieldDesc: FieldDescription, newValue: Value) {
        val field = findStaticField(fieldDesc)

        if (field.isFinal()) {
            throwBrokenCodeException(NoSuchFieldError("Can't modify a final field: $field"))
        }

        val _class = field.declaringType()
        if (_class !is jdi.ClassType) {
            throwBrokenCodeException(NoSuchFieldError("Can't a field in a non-class: $field"))
        }

        val jdiValue = newValue.asJdiValue(vm, field.`type`().asType())
        mayThrow { _class.setValue(field, jdiValue) }
    }

    private fun findMethod(methodDesc: MethodDescription, _class: jdi.ReferenceType = methodDesc.ownerType.asReferenceType()): jdi.Method {
        val method = when (_class) {
            is jdi.ClassType -> {
                val m = _class.concreteMethodByName(methodDesc.name, methodDesc.desc)
                if (m == null) listOf() else listOf(m)
            }
            else -> _class.methodsByName(methodDesc.name, methodDesc.desc)
        }
        if (method.isEmpty()) {
            throwBrokenCodeException(NoSuchMethodError("Method not found: $methodDesc"))
        }
        return method[0]
    }

    override fun invokeStaticMethod(methodDesc: MethodDescription, arguments: List<Value>): Value {
        val method = findMethod(methodDesc)
        if (!method.isStatic()) {
            throwBrokenCodeException(NoSuchMethodError("Method is not static: $methodDesc"))
        }
        val _class = method.declaringType()
        if (_class !is jdi.ClassType) throwBrokenCodeException(NoSuchMethodError("Static method is a non-class type: $method"))

        val args = mapArguments(arguments, method.safeArgumentTypes())
        val result = mayThrow { _class.invokeMethod(thread, method, args, invokePolicy) }
        return result.asValue()
    }

    override fun getField(instance: Value, fieldDesc: FieldDescription): Value {
        val field = findField(fieldDesc)
        val obj = instance.jdiObj.checkNull()

        return mayThrow { obj.getValue(field) }.asValue()
    }

    override fun setField(instance: Value, fieldDesc: FieldDescription, newValue: Value) {
        val field = findField(fieldDesc)
        val obj = instance.jdiObj.checkNull()

        val jdiValue = newValue.asJdiValue(vm, field.`type`().asType())
        mayThrow { obj.setValue(field, jdiValue) }
    }

    override fun invokeMethod(instance: Value, methodDesc: MethodDescription, arguments: List<Value>, invokespecial: Boolean): Value {
        if (invokespecial && methodDesc.name == "<init>") {
            // Constructor call
            val ctor = findMethod(methodDesc)
            val _class = (instance as NewObjectValue).asmType.asReferenceType() as jdi.ClassType
            val args = mapArguments(arguments, ctor.safeArgumentTypes())
            val result = mayThrow { _class.newInstance(thread, ctor, args, invokePolicy) }
            instance.value = result
            return result.asValue()
        }

        fun doInvokeMethod(obj: ObjectReference, method: Method, policy: Int): Value {
            val args = mapArguments(arguments, method.safeArgumentTypes())
            val result = mayThrow { obj.invokeMethod(thread, method, args, policy) }
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

    private fun mapArguments(arguments: List<Value>, expecetedTypes: List<jdi.Type>): List<jdi.Value?> {
        return arguments.zip(expecetedTypes).map {
            val (arg, expectedType) = it
            arg.asJdiValue(vm, expectedType.asType())
        }
    }

    private fun jdi.Method.safeArgumentTypes(): List<jdi.Type> {
        try {
            return argumentTypes()
        }
        catch (e: ClassNotLoadedException) {
            return argumentTypeNames()!!.map {
                name ->
                val dimensions = name.count { it == '[' }
                val baseTypeName = if (dimensions > 0) name.substring(0, name.indexOf('[')) else name

                val baseType = primitiveTypes[baseTypeName] ?: Type.getType("L$baseTypeName;").asReferenceType()

                if (dimensions == 0)
                    baseType
                else
                    Type.getType("[".repeat(dimensions) + baseType.asType().getDescriptor()).asReferenceType()
            }
        }
    }
}

fun <T> mayThrow(f: () -> T): T {
    try {
        return f()
    }
    catch (e: jdi.InvocationException) {
        throw ThrownFromEvaluatedCodeException(e.exception().asValue())
    }
}