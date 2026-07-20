// TARGET_BACKEND: JVM
// WITH_REFLECT
// LANGUAGE: +CompanionBlocks
package test

import kotlin.test.assertEquals

enum class E {
    A, B, C;

    companion {
        fun a(): E = E.A
        val b: E get() = E.B
    }
}

fun box(): String {
    assertEquals(listOf(E.A, E.B, E.C), E::values.call().toList())
    assertEquals(listOf(E.A, E.B, E.C), E::values.callBy(emptyMap()).toList())

    assertEquals(E.C, E::valueOf.call("C"))
    assertEquals(E.C, E::valueOf.callBy(mapOf(E::valueOf.parameters.single() to "C")))

    assertEquals(E.A, E::a.call())
    assertEquals(E.A, E::class.members.single { it.name == "a" }.call())
    assertEquals(E.A, E::a.callBy(emptyMap()))

    assertEquals(E.B, E::b.call())
    assertEquals(E.B, E::class.members.single { it.name == "b" }.call())
    assertEquals(E.B, E::b.callBy(emptyMap()))

    return "OK"
}
