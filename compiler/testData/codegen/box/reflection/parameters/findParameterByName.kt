// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_REFLECT
// FILE: J.java

public class J {
    public void bar(int x) {}
}

// FILE: K.kt

import kotlin.reflect.full.findParameterByName
import kotlin.test.assertEquals
import kotlin.test.assertNull

fun foo(x: Int) = x

fun box(): String {
    assertEquals(::foo.parameters.single(), ::foo.findParameterByName("x"))
    assertNull(::foo.findParameterByName("y"))

    assertNull(J::bar.findParameterByName("x"))
    assertNull(J::bar.findParameterByName("y"))

    return "OK"
}
