// FILE: 1.kt

package test

public class Z(public var s: Int)

operator inline fun Z.plusAssign(lambda: () -> Int)  {
    this.s += lambda()
}

// FILE: 2.kt

import test.*

fun test1(s: Int): Int {
    val z = Z(s)
    z += {s}
    return z.s
}

fun box(): String {
    val result = test1(11)
    if (result != 22) return "fail1: ${result}"

    return "OK"
}
