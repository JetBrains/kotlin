// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: test/J.java
package test;

public class J extends Base implements A {
    @Override
    public String getX() {
        return null;
    }
}

// FILE: box.kt
package test

import kotlin.test.assertEquals

open class Base {
    private val x: Int = 0
}

interface A {
    val x: String?
}

fun box(): String {
    assertEquals("val test.J.x: kotlin.String?", J::class.members.single { it.name == "x" }.toString())

    return "OK"
}
