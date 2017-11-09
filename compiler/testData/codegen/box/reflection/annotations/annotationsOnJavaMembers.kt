// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_REFLECT
// FILE: J.java

@Anno("J")
public class J {
    @Anno("foo")
    public static int foo = 42;

    @Anno("bar")
    public static void bar() {}

    @Anno("constructor")
    public J() {}
}

// FILE: K.kt

import kotlin.test.assertEquals

annotation class Anno(val value: String)

fun box(): String {
    assertEquals("[@Anno(value=J)]", J::class.annotations.toString())
    assertEquals("[@Anno(value=foo)]", J::foo.annotations.toString())
    assertEquals("[@Anno(value=bar)]", J::bar.annotations.toString())
    assertEquals("[@Anno(value=constructor)]", ::J.annotations.toString())

    return "OK"
}
