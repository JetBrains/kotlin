// WITH_STDLIB

import kotlin.test.*

interface I1<T> {
    fun defaultX(): T
    fun foo(x: T = defaultX()): T
}

interface I2 : I1<Int> {

}

class C : I2 {
    override fun defaultX() = 42
    override fun foo(x: Int) = x
}

fun box(): String {
    val c: I2 = C()
    assertEquals(42, c.foo())

    return "OK"
}
