package org.jetbrains.eval4j.jdi

import org.jetbrains.eval4j.*
import org.objectweb.asm.Type
import com.sun.jdi

val CLASS = Type.getType(javaClass<Class<*>>())

class JDIEval(
        private val vm: jdi.VirtualMachine,
        private val classLoader: jdi.ClassLoaderReference,
        private val thread: jdi.ThreadReference
) : Eval {
    override fun loadClass(classType: Type): Value {
        val loadedClasses = vm.classesByName(classType.getInternalName())
        if (!loadedClasses.isEmpty()) {
            return loadedClasses[0].classObject().asValue()
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

    override fun loadString(str: String): Value = vm.mirrorOf(str)!!.asValue()

    override fun newInstance(classType: Type): Value {
        return NewObjectValue(classType)
    }

    override fun isInstanceOf(value: Value, targetType: Type): Boolean {
        assert(targetType.getSort() == Type.OBJECT, "Can't check isInstanceOf() for non-object type $targetType")

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
        return array.array().getValue(index.int).asValue()
    }

    override fun setArrayElement(array: Value, index: Value, newValue: Value) {
        array.array().setValue(index.int, newValue.asJdiValue(vm))
    }

    private fun findField(fieldDesc: FieldDescription): jdi.Field {
        val _class = fieldDesc.ownerType.asReferenceType()
        val field = _class.fieldByName(fieldDesc.name)
        if (field == null) {
            throwEvalException(NoSuchFieldError("Field not found: $fieldDesc"))
        }
        return field
    }

    private fun findStaticField(fieldDesc: FieldDescription): jdi.Field {
        val field = findField(fieldDesc)
        if (!field.isStatic()) {
            throwEvalException(NoSuchFieldError("Field is not static: $fieldDesc"))
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
            throwEvalException(NoSuchFieldError("Can't modify a final field: $field"))
        }

        val _class = field.declaringType()
        if (_class !is jdi.ClassType) {
            throwEvalException(NoSuchFieldError("Can't a field in a non-class: $field"))
        }

        val jdiValue = newValue.asJdiValue(vm)
        mayThrow { _class.setValue(field, jdiValue) }
    }

    private fun findMethod(methodDesc: MethodDescription): jdi.Method {
        val _class = methodDesc.ownerType.asReferenceType()
        val method = when (_class) {
            is jdi.ClassType -> {
                val m = _class.concreteMethodByName(methodDesc.name, methodDesc.desc)
                if (m == null) listOf() else listOf(m)
            }
            else -> _class.methodsByName(methodDesc.name, methodDesc.desc)
        }
        if (method.isEmpty()) {
            throwEvalException(NoSuchMethodError("Method not found: $methodDesc"))
        }
        return method[0]
    }

    override fun invokeStaticMethod(methodDesc: MethodDescription, arguments: List<Value>): Value {
        val method = findMethod(methodDesc)
        if (!method.isStatic()) {
            throwEvalException(NoSuchMethodError("Method is not static: $methodDesc"))
        }
        val _class = method.declaringType()
        if (_class !is jdi.ClassType) throwEvalException(NoSuchMethodError("Static method is a non-class type: $method"))

        val args = arguments.map { v -> v.asJdiValue(vm) }
        val result = mayThrow { _class.invokeMethod(thread, method, args, 0) }
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

        val jdiValue = newValue.asJdiValue(vm)
        mayThrow { obj.setValue(field, jdiValue) }
    }

    override fun invokeMethod(instance: Value, methodDesc: MethodDescription, arguments: List<Value>, invokespecial: Boolean): Value {
        if (invokespecial) {
            if (methodDesc.name == "<init>") {
                // Constructor call
                val ctor = findMethod(methodDesc)
                val _class = (instance as NewObjectValue).asmType.asReferenceType() as jdi.ClassType
                val args = arguments.map { v -> v.asJdiValue(vm) }
                val result = mayThrow { _class.newInstance(thread, ctor, args, 0) }
                instance.value = result
                return result.asValue()
            }
            else {
                // TODO
                throw UnsupportedOperationException("invokespecial is not suported yet")
            }
        }
        val method = findMethod(methodDesc)

        val obj = instance.jdiObj.checkNull()
        val args = arguments.map { v -> v.asJdiValue(vm) }
        val result = mayThrow { obj.invokeMethod(thread, method, args, 0) }
        return result.asValue()
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