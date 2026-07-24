// TARGET_BACKEND: JVM
// WITH_REFLECT
// LANGUAGE: +CompanionBlocks +CompanionExtensions

import kotlin.test.assertEquals
import kotlin.reflect.KMutableProperty0

@JvmInline
value class Z(val value: String)

class A {
    companion {
        fun f(s: String): Z = Z(s)
        var p: Z = Z("p")
    }
}

companion fun A.g(s: String): Z = Z(s)
private var storage: Z = Z("q")
companion var A.q: Z
    get() = storage
    set(value) { storage = value }

fun box(): String {
    val f = A::class.members.single { it.name == "f" }
    assertEquals(Z("a"), f.call("a"))

    val p = A::class.members.single { it.name == "p" } as KMutableProperty0<Z>
    assertEquals(Z("p"), p.call())
    assertEquals(Unit, p.setter.call(Z("b")))
    assertEquals(Z("b"), p.getter.call())

    val g = A::g
    assertEquals(Z("c"), g.call("c"))

    val q = A::q
    assertEquals(Z("q"), q.call())
    assertEquals(Unit, q.setter.call(Z("d")))
    assertEquals(Z("d"), q.getter.call())

    return "OK"
}
