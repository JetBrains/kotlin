// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_REFLECT
// FILE: J.java

public class J {
    void foo(String s, int i) {}

    static void bar(J j) {}
}

// FILE: K.kt

import kotlin.test.assertEquals

fun box(): String {
    assertEquals(listOf(null, null, null), J::foo.parameters.map { it.name })
    assertEquals(listOf(null), J::bar.parameters.map { it.name })

    return "OK"
}
