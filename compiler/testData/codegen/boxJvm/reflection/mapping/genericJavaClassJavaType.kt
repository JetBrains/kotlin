// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: GenericJavaHolder.java
public class GenericJavaHolder {
    public static class Box<T> {
        public T get() { return null; }
        public void set(T value) {}
    }
    public static class Pair<A, B> {
        public A first() { return null; }
        public B second() { return null; }
    }
}

// FILE: box.kt
// Tests that KType.javaType correctly includes type parameters for generic Java classes.

import kotlin.reflect.jvm.javaType
import kotlin.reflect.full.*
import kotlin.test.*

fun box(): String {
    // Box<T>.get() → return type T; represented as a TypeVariable "T"
    val getMethod = GenericJavaHolder.Box::class.memberFunctions.first { it.name == "get" }
    val getJavaType = getMethod.returnType.javaType.typeName
    assertEquals("T", getJavaType,
        "Box.get() return javaType should be 'T', got: $getJavaType")

    // Box<T>.set(T) → parameter type T
    val setMethod = GenericJavaHolder.Box::class.memberFunctions.first { it.name == "set" }
    val setParamType = setMethod.valueParameters.first().type.javaType.typeName
    assertEquals("T", setParamType,
        "Box.set() parameter javaType should be 'T', got: $setParamType")

    // Pair<A, B>.first() → A, second() → B (distinct type variables)
    val firstMethod = GenericJavaHolder.Pair::class.memberFunctions.first { it.name == "first" }
    assertEquals("A", firstMethod.returnType.javaType.typeName,
        "Pair.first() return javaType should be 'A'")

    val secondMethod = GenericJavaHolder.Pair::class.memberFunctions.first { it.name == "second" }
    assertEquals("B", secondMethod.returnType.javaType.typeName,
        "Pair.second() return javaType should be 'B'")

    // For the class itself as a type argument, javaType should include the parameter
    val boxSupertype = GenericJavaHolder.Box::class.supertypes.firstOrNull()
    // supertypes just shows Any here, but declaring class via members is tested above

    return "OK"
}
