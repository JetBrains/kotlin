// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.*
import kotlin.test.assertEquals

class C(val x: Int, var y: Int)

val C.xx: Int
    get() = x

var C.yy: Int
    get() = y
    set(value) { y = value }


val c = C(1, 2)

val c_xx = c::xx
val c_y = c::y
val c_yy = c::yy

fun box(): String {
    assertEquals(1, c_xx.getter())
    assertEquals(1, c_xx.getter.call())
    assertEquals(2, c_yy.getter())
    assertEquals(2, c_yy.getter.call())

    c_y.setter(10)
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
