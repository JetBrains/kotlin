// WITH_STDLIB

import kotlin.test.*

interface I {
    inline class IC(val x: Int)
}

interface I2 {
    inline class IC(val x: Int)
}

fun box(): String {
    assertEquals(42, I.IC(42).x)
    assertEquals(117, I2.IC(117).x)

    return "OK"
}
