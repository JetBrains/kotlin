// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// WITH_REFLECT
// FILE: J.java

public class J {
    public J() {
    }

    public void member(String s) {
    }

    public static void staticMethod(int x) {
    }
}

// FILE: K.kt

import kotlin.reflect.full.*
import kotlin.test.assertEquals

fun box(): String {
    assertEquals(listOf("equals", "hashCode", "member", "staticMethod", "toString"), J::class.members.map { it.name }.sorted())
    assertEquals(listOf("equals", "hashCode", "member", "staticMethod", "toString"), J::class.functions.map { it.name }.sorted())
    assertEquals(listOf("member", "staticMethod"), J::class.declaredFunctions.map { it.name }.sorted())

    assertEquals(1, J::class.constructors.size)

    return "OK"
}
