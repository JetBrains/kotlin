// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: test/J.java
package test;

public class J {
    public static final J INSTANCE = new J();
}

// FILE: box.kt
package test

import kotlin.test.assertEquals

object Obj {
    fun foo() = 1
}

class A {
    companion object {
        fun foo() = 2
    }
}

class B {
    companion object Factory {
        fun foo() = 3
    }
}

class C

fun box(): String {
    assertEquals(1, Obj::class.objectInstance!!.foo())
    assertEquals(2, A.Companion::class.objectInstance!!.foo())
    assertEquals(3, B.Factory::class.objectInstance!!.foo())

    assertEquals(null, C::class.objectInstance)
    assertEquals(null, String::class.objectInstance)
    assertEquals(Unit, Unit::class.objectInstance)
    assertEquals(null, object {}::class.objectInstance)
    assertEquals(null, J::class.objectInstance)

    return "OK"
}
