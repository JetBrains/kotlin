// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: KotlinFunctionImplementor.java
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;

public class KotlinFunctionImplementor {
    public static class IntToString implements Function1<Integer, String> {
        @Override public String invoke(Integer value) { return String.valueOf(value); }
    }
    public static class StringAndIntToBoolean implements Function2<String, Integer, Boolean> {
        @Override public Boolean invoke(String s, Integer n) { return s.length() == n; }
    }
}

// FILE: box.kt
// Tests calling `invoke` of a Java class implementing a Kotlin function type with boxed type arguments.
// The compiler loads such parameters as flexible, e.g. `IntToString().invoke(null)` compiles.

import kotlin.reflect.KClass
import kotlin.test.assertEquals

private fun KClass<*>.invoke() = members.single { it.name == "invoke" }

fun box(): String {
    val invoke1 = KotlinFunctionImplementor.IntToString::class.invoke()
    val invoke2 = KotlinFunctionImplementor.StringAndIntToBoolean::class.invoke()

    assertEquals("null", invoke1.call(KotlinFunctionImplementor.IntToString(), null))
    assertEquals(true, invoke2.call(KotlinFunctionImplementor.StringAndIntToBoolean(), "ab", 2))

    return "OK"
}
