// WITH_STDLIB

import kotlin.test.*

class A(y: Int) {
    var x = y
}

val A.z get() = this.x

fun box(): String {
    val p1 = A::z
    assertEquals(42, p1.get(A(42)))
    val a = A(117)
    val p2 = a::z
    assertEquals(117, p2.get())

    return "OK"
}
