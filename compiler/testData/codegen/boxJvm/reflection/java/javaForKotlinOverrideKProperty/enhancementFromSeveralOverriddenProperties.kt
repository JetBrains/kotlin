// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: test/J.java
package test;

import java.util.List;

public class J implements A, B<String> {
    @Override
    public String getX() {
        return "OK";
    }

    @Override
    public List<String> getY() {
        return null;
    }
}

// FILE: box.kt
package test

import kotlin.test.assertEquals

interface A {
    val x: String?
    val y: List<String>?
}

interface B<T> {
    val x: T & Any
    val y: MutableList<T & Any>
}

fun box(): String {
    assertEquals("val test.J.x: kotlin.String", J::class.members.single { it.name == "x" }.toString())
    assertEquals("val test.J.y: kotlin.collections.MutableList<kotlin.String>", J::class.members.single { it.name == "y" }.toString())

    return "OK"
}
