// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: KotlinFunctionImplementor.java
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;

public class KotlinFunctionImplementor {
    public static class IntToString implements Function1<Integer, String> {
        @Override public String invoke(Integer value) { return value.toString(); }
    }
    public static class StringAndIntToBoolean implements Function2<String, Integer, Boolean> {
        @Override public Boolean invoke(String s, Integer n) { return s.length() == n; }
    }
}

// FILE: box.kt
// Tests that a Java class implementing a Kotlin functional interface has its invoke
// parameter and return types reflected as proper Kotlin types (non-nullable, not platform).

import kotlin.reflect.full.*
import kotlin.test.*

fun box(): String {
    // Function1<Integer, String> implementor: invoke(Integer) → String
    val invoke1 = KotlinFunctionImplementor.IntToString::class
        .memberFunctions.first { it.name == "invoke" && !it.isAbstract }

    val paramType = invoke1.valueParameters.first().type.toString()
    assertFalse(paramType.endsWith("!"),
        "invoke parameter type should not be a platform type, got: $paramType")
    assertEquals("kotlin.Int", paramType,
        "invoke parameter should be kotlin.Int (not kotlin.Int! or java.lang.Integer)")

    val returnType = invoke1.returnType.toString()
    assertFalse(returnType.endsWith("!"),
        "invoke return type should not be a platform type, got: $returnType")
    assertEquals("kotlin.String", returnType,
        "invoke return should be kotlin.String (not kotlin.String!)")

    // Function2<String, Integer, Boolean> implementor
    val invoke2 = KotlinFunctionImplementor.StringAndIntToBoolean::class
        .memberFunctions.first { it.name == "invoke" && !it.isAbstract }

    assertEquals(2, invoke2.valueParameters.size)
    assertEquals("kotlin.String", invoke2.valueParameters[0].type.toString())
    assertEquals("kotlin.Int",    invoke2.valueParameters[1].type.toString())
    assertEquals("kotlin.Boolean", invoke2.returnType.toString())

    return "OK"
}
