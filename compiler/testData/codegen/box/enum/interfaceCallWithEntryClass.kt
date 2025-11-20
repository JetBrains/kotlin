// WITH_STDLIB

import kotlin.test.*

interface A {
    fun f(): String
}

enum class Zzz: A {
    Z1 {
        override fun f() = "z1"
    },

    Z2 {
        override fun f() = "z2"
    };

    override fun f() = ""
}

fun box(): String {
    assertEquals("z1z2", Zzz.Z1.f() + Zzz.Z2.f())
    val a1: A = Zzz.Z1
    val a2: A = Zzz.Z2
    assertEquals("z1z2", a1.f() + a2.f())

    return "OK"
}
