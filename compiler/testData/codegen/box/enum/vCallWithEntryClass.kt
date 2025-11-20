// WITH_STDLIB

import kotlin.test.*

enum class Zzz {
    Z1 {
        override fun f() = "z1"
    },

    Z2 {
        override fun f() = "z2"
    };

    open fun f() = ""
}

fun box(): String {
    assertEquals("z1z2", Zzz.Z1.f() + Zzz.Z2.f())

    return "OK"
}
