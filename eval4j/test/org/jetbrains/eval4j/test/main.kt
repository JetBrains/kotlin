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

package org.jetbrains.eval4j.test

import org.jetbrains.org.objectweb.asm.*
import org.jetbrains.org.objectweb.asm.Opcodes.*
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import java.lang.reflect.Modifier
import org.jetbrains.eval4j.*
import org.junit.Assert.*
import junit.framework.TestSuite
import junit.framework.TestCase
import java.lang.reflect.Method
import java.lang.reflect.Field
import java.lang.reflect.Constructor
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Array as JArray
import org.jetbrains.org.objectweb.asm.tree.analysis.Frame

fun suite(): TestSuite = buildTestSuite {
    methodNode, ownerClass, expected ->
    object : TestCase(getTestName(methodNode.name)) {

            override fun runTest() {
                if (!isIgnored(methodNode)) {
                    val value = interpreterLoop(
                            methodNode,
                            initFrame(
                                    ownerClass.getInternalName(),
                                    methodNode
                            ),
                            REFLECTION_EVAL
                    )
                    
                    if (expected is ExceptionThrown && value is ExceptionThrown) {
                        assertEquals(expected.exception.toString(), value.exception.toString())
                    }
                    else {
                        assertEquals(expected.toString(), value.toString())
                    }
                }
            }

            private fun isIgnored(methodNode: MethodNode): Boolean {
                return methodNode.visibleAnnotations?.any {
                    val annotationDesc = it.desc
                    annotationDesc != null &&
                        Type.getType(annotationDesc) == Type.getType(javaClass<IgnoreInReflectionTests>())
                } ?: false
            }
        }
}

fun Class<*>.getInternalName(): String = Type.getType(this).getInternalName()

fun initFrame(
        owner: String,
        m: MethodNode
): Frame<Value> {
    val current = Frame<Value>(m.maxLocals, m.maxStack)
    current.setReturn(makeNotInitializedValue(Type.getReturnType(m.desc)))

    var local = 0
    if ((m.access and ACC_STATIC) == 0) {
        val ctype = Type.getObjectType(owner)
        val newInstance = REFLECTION_EVAL.newInstance(ctype)
        val thisValue = REFLECTION_EVAL.invokeMethod(newInstance, MethodDescription(owner, "<init>", "()V", false), listOf(), true)
        current.setLocal(local++, thisValue)
    }

    val args = Type.getArgumentTypes(m.desc)
    for (i in 0..args.size - 1) {
        current.setLocal(local++, makeNotInitializedValue(args[i]))
        if (args[i].getSize() == 2) {
            current.setLocal(local++, NOT_A_VALUE)
        }
    }

    while (local < m.maxLocals) {
        current.setLocal(local++, NOT_A_VALUE)
    }

    return current
}

fun objectToValue(obj: Any?, expectedType: Type): Value {
    return when (expectedType.getSort()) {
        Type.VOID -> VOID_VALUE
        Type.BOOLEAN -> boolean(obj as Boolean)
        Type.BYTE -> byte(obj as Byte)
        Type.SHORT -> short(obj as Short)
        Type.CHAR -> char(obj as Char)
        Type.INT -> int(obj as Int)
        Type.LONG -> long(obj as Long)
        Type.DOUBLE -> double(obj as Double)
        Type.FLOAT -> float(obj as Float)
        Type.OBJECT -> if (obj == null) NULL_VALUE else ObjectValue(obj, expectedType)
        else -> throw UnsupportedOperationException("Unsupported result type: $expectedType")
    }
}

object REFLECTION_EVAL : Eval {

    val lookup = ReflectionLookup(javaClass<ReflectionLookup>().getClassLoader()!!)

    override fun loadClass(classType: Type): Value {
        return ObjectValue(findClass(classType), Type.getType(javaClass<Class<*>>()))
    }

    override fun loadString(str: String): Value = ObjectValue(str, Type.getType(javaClass<String>()))

    override fun newInstance(classType: Type): Value {
        return NewObjectValue(classType)
    }

    override fun isInstanceOf(value: Value, targetType: Type): Boolean {
        val _class = findClass(targetType)
        return _class.isInstance(value.obj())
    }

    [suppress("UNCHECKED_CAST")]
    override fun newArray(arrayType: Type, size: Int): Value {
        return ObjectValue(JArray.newInstance(findClass(arrayType).getComponentType() as Class<Any>, size), arrayType)
    }

    override fun newMultiDimensionalArray(arrayType: Type, dimensionSizes: List<Int>): Value {
        return ObjectValue(ArrayHelper.newMultiArray(findClass(arrayType.getElementType()), *dimensionSizes.copyToArray()), arrayType)
    }

    override fun getArrayLength(array: Value): Value {
        return int(JArray.getLength(array.obj().checkNull()))
    }

    override fun getArrayElement(array: Value, index: Value): Value {
        val asmType = array.asmType
        val elementType = if (asmType.getDimensions() == 1) asmType.getElementType() else Type.getType(asmType.getDescriptor().substring(1))
        val arr = array.obj().checkNull()
        val ind = index.int
        return mayThrow {
            when (elementType.getSort()) {
                Type.BOOLEAN -> boolean(JArray.getBoolean(arr, ind))
                Type.BYTE -> byte(JArray.getByte(arr, ind))
                Type.SHORT -> short(JArray.getShort(arr, ind))
                Type.CHAR -> char(JArray.getChar(arr, ind))
                Type.INT -> int(JArray.getInt(arr, ind))
                Type.LONG -> long(JArray.getLong(arr, ind))
                Type.FLOAT -> float(JArray.getFloat(arr, ind))
                Type.DOUBLE -> double(JArray.getDouble(arr, ind))
                Type.OBJECT,
                Type.ARRAY -> {
                    val value = JArray.get(arr, ind)
                    if (value == null) NULL_VALUE else ObjectValue(value, Type.getType(value.javaClass))
                }
                else -> throw UnsupportedOperationException("Unsupported array element type: $elementType")
            }
        }
    }

    override fun setArrayElement(array: Value, index: Value, newValue: Value) {
        val arr = array.obj().checkNull()
        val ind = index.int
        if (array.asmType.getDimensions() > 1) {
            JArray.set(arr, ind, newValue.obj())
            return
        }
        val elementType = array.asmType.getElementType()
        mayThrow {
            when (elementType.getSort()) {
                Type.BOOLEAN -> JArray.setBoolean(arr, ind, newValue.boolean)
                Type.BYTE -> JArray.setByte(arr, ind, newValue.int.toByte())
                Type.SHORT -> JArray.setShort(arr, ind, newValue.int.toShort())
                Type.CHAR -> JArray.setChar(arr, ind, newValue.int.toChar())
                Type.INT -> JArray.setInt(arr, ind, newValue.int)
                Type.LONG -> JArray.setLong(arr, ind, newValue.long)
                Type.FLOAT -> JArray.setFloat(arr, ind, newValue.float)
                Type.DOUBLE -> JArray.setDouble(arr, ind, newValue.double)
                Type.OBJECT,
                Type.ARRAY -> {
                    JArray.set(arr, ind, newValue.obj())
                }
                else -> throw UnsupportedOperationException("Unsupported array element type: $elementType")
            }
        }
    }

    fun <T> mayThrow(f: () -> T): T {
        try {
            try {
                return f()
            }
            catch (ite: InvocationTargetException) {
                    throw ite.getCause() ?: ite
            }
        }
        catch (e: Throwable) {
            throw ThrownFromEvaluatedCodeException(ObjectValue(e, Type.getType(e.javaClass)))
        }
    }

    override fun getStaticField(fieldDesc: FieldDescription): Value {
        val field = findStaticField(fieldDesc)

        val result = mayThrow {field.get(null)}
        return objectToValue(result, fieldDesc.fieldType)
    }

    override fun setStaticField(fieldDesc: FieldDescription, newValue: Value) {
        val field = findStaticField(fieldDesc)
        val obj = newValue.obj(fieldDesc.fieldType)
        mayThrow {field.set(null, obj)}
    }

    fun findStaticField(fieldDesc: FieldDescription): Field {
        assertTrue(fieldDesc.isStatic)
        val field = findClass(fieldDesc).findField(fieldDesc)
        assertNotNull("Field not found: $fieldDesc", field)
        assertTrue("Field is not static: $field", (field!!.getModifiers() and Modifier.STATIC) != 0)
        return field
    }

    override fun invokeStaticMethod(methodDesc: MethodDescription, arguments: List<Value>): Value {
        assertTrue(methodDesc.isStatic)
        val method = findClass(methodDesc).findMethod(methodDesc)
        assertNotNull("Method not found: $methodDesc", method)
        val args = mapArguments(arguments, methodDesc.parameterTypes).copyToArray()
        val result = mayThrow {method!!.invoke(null, *args)}
        return objectToValue(result, methodDesc.returnType)
    }

    fun findClass(memberDesc: MemberDescription): Class<Any> = findClass(Type.getObjectType(memberDesc.ownerInternalName))

    fun findClass(asmType: Type): Class<Any> {
        val owner = lookup.findClass(asmType)
        assertNotNull("Class not found: ${asmType}", owner)
        return owner as Class<Any>
    }

    override fun getField(instance: Value, fieldDesc: FieldDescription): Value {
        val obj = instance.obj().checkNull()
        val field = findInstanceField(obj, fieldDesc)

        return objectToValue(mayThrow {field.get(obj)}, fieldDesc.fieldType)
    }

    override fun setField(instance: Value, fieldDesc: FieldDescription, newValue: Value) {
        val obj = instance.obj().checkNull()
        val field = findInstanceField(obj, fieldDesc)

        val newObj = newValue.obj(fieldDesc.fieldType)
        mayThrow {field.set(obj, newObj)}
    }

    fun findInstanceField(obj: Any, fieldDesc: FieldDescription): Field {
        val _class = obj.javaClass
        val field = _class.findField(fieldDesc)
        assertNotNull("Field not found: $fieldDesc", field)
        return field!!
    }

    override fun invokeMethod(instance: Value, methodDesc: MethodDescription, arguments: List<Value>, invokespecial: Boolean): Value {
        if (invokespecial) {
            if (methodDesc.name == "<init>") {
                // Constructor call
                [suppress("UNCHECKED_CAST")]
                val _class = findClass((instance as NewObjectValue).asmType)
                val ctor = _class.findConstructor(methodDesc)
                assertNotNull("Constructor not found: $methodDesc", ctor)
                val args = mapArguments(arguments, methodDesc.parameterTypes).copyToArray()
                val result = mayThrow {ctor!!.newInstance(*args)}
                instance.value = result
                return objectToValue(result, instance.asmType)
            }
            else {
                // TODO
                throw UnsupportedOperationException("invokespecial is not suported in reflection eval")
            }
        }
        val obj = instance.obj().checkNull()
        val method = obj.javaClass.findMethod(methodDesc)
        assertNotNull("Method not found: $methodDesc", method)
        val args = mapArguments(arguments, methodDesc.parameterTypes).copyToArray()
        val result = mayThrow {method!!.invoke(obj, *args)}
        return objectToValue(result, methodDesc.returnType)
    }

    private fun mapArguments(arguments: List<Value>, expecetedTypes: List<Type>): List<Any?> {
        return arguments.zip(expecetedTypes).map {
            val (arg, expectedType) = it
            arg.obj(expectedType)
        }
    }
}

class ReflectionLookup(val classLoader: ClassLoader) {
    [suppress("UNCHECKED_CAST")]
    fun findClass(asmType: Type): Class<*>? {
        return when (asmType.getSort()) {
            Type.BOOLEAN -> java.lang.Boolean.TYPE
            Type.BYTE -> java.lang.Byte.TYPE
            Type.SHORT -> java.lang.Short.TYPE
            Type.CHAR -> java.lang.Character.TYPE
            Type.INT -> java.lang.Integer.TYPE
            Type.LONG -> java.lang.Long.TYPE
            Type.FLOAT -> java.lang.Float.TYPE
            Type.DOUBLE -> java.lang.Double.TYPE
            Type.OBJECT -> classLoader.loadClass(asmType.getInternalName().replace('/', '.'))
            Type.ARRAY -> Class.forName(asmType.getDescriptor().replace('/', '.'))
            else -> throw UnsupportedOperationException("Unsupported type: $asmType")
        }
    }
}

[suppress("UNCHECKED_CAST")]
fun Class<Any>.findMethod(methodDesc: MethodDescription): Method? {
    for (declared in getDeclaredMethods()) {
        if (methodDesc.matches(declared)) return declared
    }

    val fromSuperClass = (getSuperclass() as Class<Any>).findMethod(methodDesc)
    if (fromSuperClass != null) return fromSuperClass

    for (supertype in getInterfaces()) {
        val fromSuper = (supertype as Class<Any>).findMethod(methodDesc)
        if (fromSuper != null) return fromSuper
    }

    return null
}

[suppress("UNCHECKED_CAST")]
fun Class<Any>.findConstructor(methodDesc: MethodDescription): Constructor<Any?>? {
    for (declared in getDeclaredConstructors()) {
        if (methodDesc.matches(declared)) return declared as Constructor<Any?>
    }
    return null
}

fun MethodDescription.matches(ctor: Constructor<*>): Boolean {
    val methodParams = ctor.getParameterTypes()!!
    if (parameterTypes.size() != methodParams.size) return false
    for ((i, p) in parameterTypes.withIndices()) {
        if (!p.matches(methodParams[i])) return false
    }

    return true;
}

fun MethodDescription.matches(method: Method): Boolean {
    if (name != method.getName()) return false

    val methodParams = method.getParameterTypes()!!
    if (parameterTypes.size() != methodParams.size) return false
    for ((i, p) in parameterTypes.withIndices()) {
        if (!p.matches(methodParams[i])) return false
    }

    return returnType.matches(method.getReturnType()!!)
}

[suppress("UNCHECKED_CAST")]
fun Class<Any>.findField(fieldDesc: FieldDescription): Field? {
    for (declared in getDeclaredFields()) {
        if (fieldDesc.matches(declared)) return declared
    }

    val superclass = getSuperclass()
    if (superclass != null) {
        val fromSuperClass = (superclass as Class<Any>).findField(fieldDesc)
        if (fromSuperClass != null) return fromSuperClass
    }

    for (supertype in getInterfaces()) {
        val fromSuper = (supertype as Class<Any>).findField(fieldDesc)
        if (fromSuper != null) return fromSuper
    }

    return null
}

fun FieldDescription.matches(field: Field): Boolean {
    if (name != field.getName()) return false

    return fieldType.matches(field.getType()!!)
}

fun Type.matches(_class: Class<*>): Boolean = this == Type.getType(_class)