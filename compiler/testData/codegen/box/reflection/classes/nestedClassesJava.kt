// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// WITH_REFLECT
// FILE: J.java

public class J {
    public class Inner {}

    public static class Nested {}

    private static class PrivateNested {}

    // This anonymous class should not appear in 'nestedClasses'
    private final Object o = new Object() {};
}

// FILE: K.kt

import kotlin.test.assertEquals

fun box(): String {
    assertEquals(listOf("Inner", "Nested", "PrivateNested"), J::class.nestedClasses.map { it.simpleName!! }.sorted())

    return "OK"
}
