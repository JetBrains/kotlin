// TARGET_BACKEND: JVM
// WITH_REFLECT
// LANGUAGE: +CompanionBlocks +CompanionExtensions

import kotlin.test.assertEquals

class A {
    companion {
        fun f(x: Int): Int = x + 1
        var p: String = "a"
    }
}

companion fun A.g(x: Int): Int = x + 10
private var storage = "b"
companion var A.q: String
    get() = storage
    set(value) { storage = value }

fun box(): String {
    assertEquals(2, A::f.call(1))
    assertEquals(2, A::class.members.single { it.name == "f" }.call(1))

    assertEquals("a", A::p.call())
    assertEquals("a", A::class.members.single { it.name == "p" }.call())
    assertEquals(Unit, A::p.setter.call("p"))
    assertEquals("p", A::p.getter.call())

    assertEquals(11, A::g.call(1))
    assertEquals("b", A::q.call())
    assertEquals(Unit, A::q.setter.call("q"))
    assertEquals("q", A::q.getter.call())

    return "OK"
}
