// WITH_STDLIB

import kotlin.test.*

enum class Zzz(val zzz: String, val x: Int) {
    Z1("z1", 1),
    Z2("z2", 2)
}

fun box(): String {
    assertEquals("z12", Zzz.Z1.zzz + Zzz.Z2.x)

    return "OK"
}
