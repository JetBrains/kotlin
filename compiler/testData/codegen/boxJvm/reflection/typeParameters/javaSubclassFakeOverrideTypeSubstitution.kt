// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: TypedJavaBase.java
public class TypedJavaBase<T> {
    public T value() { return null; }
    public void setValue(T t) {}
    public java.util.List<T> getList() { return null; }
}

// FILE: box.kt
// Tests that a Kotlin class extending a generic Java class has its inherited fake override
// methods with type parameters correctly substituted with the concrete bound type.

import kotlin.reflect.full.*
import kotlin.reflect.jvm.javaType
import kotlin.test.*

class KotlinExtendsTyped : TypedJavaBase<String>()
class KotlinExtendsTypedInt : TypedJavaBase<Int>()

fun box(): String {
    // value() return type should be String, not T
    val valueFn = KotlinExtendsTyped::class.memberFunctions.firstOrNull { it.name == "value" }
        ?: return "Fail: 'value' not found in KotlinExtendsTyped.memberFunctions"

    val returnJavaType = valueFn.returnType.javaType.typeName
    assertFalse(returnJavaType == "T",
        "KotlinExtendsTyped.value() return javaType must not be raw 'T', got: $returnJavaType")
    assertEquals("java.lang.String", returnJavaType,
        "KotlinExtendsTyped.value() return javaType should be java.lang.String")

    // setValue(T) parameter type should be String
    val setValueFn = KotlinExtendsTyped::class.memberFunctions.firstOrNull { it.name == "setValue" }
    if (setValueFn != null) {
        val paramType = setValueFn.valueParameters.firstOrNull()?.type?.javaType?.typeName
        if (paramType != null) {
            assertFalse(paramType == "T",
                "KotlinExtendsTyped.setValue() param must not be 'T', got: $paramType")
        }
    }

    // For Int bound: value() should return int/Integer
    val intValueFn = KotlinExtendsTypedInt::class.memberFunctions.firstOrNull { it.name == "value" }
    if (intValueFn != null) {
        val intReturnType = intValueFn.returnType.javaType.typeName
        assertFalse(intReturnType == "T",
            "KotlinExtendsTypedInt.value() return must not be raw 'T', got: $intReturnType")
    }

    return "OK"
}
