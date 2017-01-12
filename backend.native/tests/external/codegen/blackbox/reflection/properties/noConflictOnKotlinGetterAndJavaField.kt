// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_REFLECT
// FILE: J.java

public class J {
    public String foo = "";
}

// FILE: K.kt

import kotlin.test.*

class K : J() {
    fun getFoo(): String = "K"
}

fun box(): String {
    val j = J()
    val x = J::foo
    x.set(j, "J")
    assertEquals("J", x.get(j))

    val k = K()
    val y = K::foo
    y.set(k, "K")
    assertEquals("K", y.get(k))
    assertEquals("K", x.get(k))

    val z = K::getFoo
    assertEquals("K", z.invoke(k))

    return "OK"
}
