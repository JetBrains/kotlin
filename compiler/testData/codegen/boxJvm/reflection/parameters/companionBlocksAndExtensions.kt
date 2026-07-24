// TARGET_BACKEND: JVM
// WITH_REFLECT
// LANGUAGE: +CompanionBlocks +CompanionExtensions

import kotlin.test.assertEquals

class A {
    companion {
        fun f(x: Int): Int = x
        var p: String = ""
    }
}

companion fun A.g(x: Int): Int = x
companion var A.q: String
    get() = ""
    set(value) {}

fun box(): String {
    assertEquals("[parameter #0 x of fun f(kotlin.Int): kotlin.Int]", A::f.parameters.toString())
    assertEquals("[parameter #0 x of fun f(kotlin.Int): kotlin.Int]", A::class.members.single { it.name == "f" }.parameters.toString())

    assertEquals("[]", A::p.parameters.toString())
    assertEquals("[]", A::class.members.single { it.name == "p" }.parameters.toString())
    assertEquals("[]", A::p.getter.parameters.toString())
    assertEquals("[parameter #0 null of fun `<set-p>`(kotlin.String): kotlin.Unit]", A::p.setter.parameters.toString())

    assertEquals("[parameter #0 x of fun g(kotlin.Int): kotlin.Int]", A::g.parameters.toString())
    assertEquals("[]", A::q.parameters.toString())
    assertEquals("[]", A::q.getter.parameters.toString())
    assertEquals("[parameter #0 value of fun `<set-q>`(kotlin.String): kotlin.Unit]", A::q.setter.parameters.toString())

    return "OK"
}
