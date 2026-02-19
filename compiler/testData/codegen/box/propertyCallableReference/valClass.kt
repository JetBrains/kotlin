// WITH_STDLIB

import kotlin.test.*

class A(val x: Int)

fun box(): String {
    val p1 = A::x
    assertEquals(42, p1.get(A(42)))
    val a = A(117)
    val p2 = a::x
    assertEquals(117, p2.get())

    return "OK"
}
