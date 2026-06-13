// TARGET_BACKEND: JVM
// WITH_REFLECT
// LANGUAGE: +CompanionBlocksAndExtensions

import kotlin.test.assertEquals

class A {
    companion {
        fun f(x: Int = 1): Int = x + 1
        var p: String = "a"
    }
}

companion fun A.g(x: Int = 1): Int = x + 10
private var storage = "b"
companion var A.q: String
    get() = storage
    set(value) { storage = value }

fun box(): String {
    assertEquals(2, A::f.callBy(emptyMap()))
    assertEquals(2, A::class.members.single { it.name == "f" }.callBy(emptyMap()))

    assertEquals("a", A::p.callBy(emptyMap()))
    assertEquals("a", A::class.members.single { it.name == "p" }.callBy(emptyMap()))
    assertEquals(Unit, A::p.setter.callBy(mapOf(A::p.setter.parameters.single() to "p")))
    assertEquals("p", A::p.getter.callBy(emptyMap()))

    assertEquals(11, A::g.callBy(emptyMap()))
    assertEquals("b", A::q.callBy(emptyMap()))
    assertEquals(Unit, A::q.setter.callBy(mapOf(A::q.setter.parameters.single() to "q")))
    assertEquals("q", A::q.getter.callBy(emptyMap()))

    return "OK"
}
