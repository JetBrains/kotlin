// TARGET_BACKEND: JVM
// WITH_REFLECT
// LANGUAGE: +CompanionBlocksAndExtensions

import kotlin.test.assertEquals
import kotlin.reflect.KMutableProperty0

@JvmInline
value class Z(val value: Int)

class A {
    companion {
        fun f(s: Int): Z = Z(s)
        var p: Z = Z(10)
    }
}

companion fun A.g(s: Int): Z = Z(s)
private var storage: Z = Z(20)
companion var A.q: Z
    get() = storage
    set(value) { storage = value }

fun box(): String {
    val f = A::class.members.single { it.name == "f" }
    assertEquals(Z(1), f.call(1))

    val p = A::class.members.single { it.name == "p" } as KMutableProperty0<Z>
    assertEquals(Z(10), p.call())
    assertEquals(Unit, p.setter.call(Z(11)))
    assertEquals(Z(11), p.getter.call())

    val g = A::g
    assertEquals(Z(2), g.call(2))

    val q = A::q
    assertEquals(Z(20), q.call())
    assertEquals(Unit, q.setter.call(Z(21)))
    assertEquals(Z(21), q.getter.call())

    return "OK"
}
