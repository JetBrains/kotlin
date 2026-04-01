// WITH_STDLIB

import kotlin.test.*

class A {
    var x = 0
}

fun box(): String {
    var sum1 = 0
    var sum2 = 0
    for (i in 0 until 10) {
        val a = A()
        sum1 += a.x
        a.x = i
        sum2 += a.x
    }
    assertEquals(0, sum1)
    assertEquals(45, sum2)

    return "OK"
}
