// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_REFLECT
// FILE: J.java

public class J {
    static String x;
}

// FILE: K.kt

import kotlin.test.assertEquals

fun box(): String {
    val f = J::x
    assertEquals("x", f.name)

    assertEquals(f, J::class.members.single { it.name == "x" })

    f.set("OK")
    assertEquals("OK", J.x)
    assertEquals("OK", f.getter())

    return f.get()
}
