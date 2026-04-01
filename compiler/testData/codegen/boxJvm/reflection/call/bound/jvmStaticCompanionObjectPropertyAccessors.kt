// TARGET_BACKEND: JVM

// WITH_REFLECT

import kotlin.reflect.*
import kotlin.test.assertEquals

class Host {
    companion object {
        @JvmStatic val x = 1
        @JvmStatic var y = 2

        @JvmStatic val xx: Int
            get() = x

        @JvmStatic var yy: Int
            get() = y
            set(value) { y = value }
    }
}

val c_x = Host.Companion::x
val c_xx = Host.Companion::xx
val c_y = Host.Companion::y
val c_yy = Host.Companion::yy

fun box(): String {
    assertEquals(1, c_x.getter())
    assertEquals(1, c_x.getter.call())
    assertEquals(1, c_xx.getter())
    assertEquals(1, c_xx.getter.call())
    assertEquals(2, c_y.getter())
    assertEquals(2, c_y.getter.call())
    assertEquals(2, c_yy.getter())
    assertEquals(2, c_yy.getter.call())

    c_y.setter(10)
    assertEquals(10, c_y.getter())
    assertEquals(10, c_yy.getter())

    c_yy.setter(20)
    assertEquals(20, c_y.getter())
    assertEquals(20, c_yy.getter())

    c_y.setter.call(100)
    assertEquals(100, c_yy.getter.call())

    c_yy.setter.call(200)
    assertEquals(200, c_y.getter.call())

    return "OK"
}
