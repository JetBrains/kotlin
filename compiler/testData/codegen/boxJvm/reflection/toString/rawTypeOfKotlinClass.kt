// WITH_REFLECT
// TARGET_BACKEND: JVM
// FILE: test/J.java
package test;

import java.util.*;

public class J {
    public void inv(Inv a) {}
    public void invNumber(InvNumber a) {}
    public void invNullableNumber(InvNullableNumber a) {}
    public void invRecursive(InvRecursive a) {}
    public void invDependent(InvDependent a) {}
    public void inOut(InOut a) {}
    public void list(List a) {}
    public void comparable(Comparable a) {}
}

// FILE: main.kt
package test

import kotlin.reflect.full.*
import kotlin.test.assertEquals

class Inv<T>
class InvNumber<T : Number>
class InvNullableNumber<T : Number?>
class InvRecursive<T : Inv<T>>
class InvDependent<T : CharSequence, U : T>
class InOut<in T : Number, out U : Any>

fun box(): String {
    val useK1 = Class.forName("kotlin.reflect.jvm.internal.SystemPropertiesKt").getMethod("getUseK1Implementation").invoke(null) == true

    val parameterTypes = J::class.declaredFunctions.associate { it.name to it.parameters[1].type.toString() }
    assertEquals("test.Inv<(raw) kotlin.Any?>", parameterTypes["inv"])
    assertEquals("test.InvNumber<(raw) kotlin.Number>", parameterTypes["invNumber"])
    assertEquals("test.InvNullableNumber<(raw) kotlin.Number?>", parameterTypes["invNullableNumber"])
    // The legacy implementation incorrectly erases the self-referencing bound to a star projection.
    assertEquals(if (useK1) "test.InvRecursive<(raw) *>" else "test.InvRecursive<(raw) test.Inv<*>>", parameterTypes["invRecursive"])
    assertEquals("test.InvDependent<(raw) kotlin.CharSequence, (raw) kotlin.CharSequence>", parameterTypes["invDependent"])
    assertEquals("test.InOut<(raw) kotlin.Number, (raw) kotlin.Any>", parameterTypes["inOut"])
    assertEquals("kotlin.collections.MutableList<(raw) kotlin.Any?>", parameterTypes["list"])
    assertEquals("kotlin.Comparable<(raw) kotlin.Any?>", parameterTypes["comparable"])
    return "OK"
}
