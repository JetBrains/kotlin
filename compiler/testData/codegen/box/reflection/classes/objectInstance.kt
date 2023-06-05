// TARGET_BACKEND: JVM
// WITH_REFLECT

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

    return "OK"
}
