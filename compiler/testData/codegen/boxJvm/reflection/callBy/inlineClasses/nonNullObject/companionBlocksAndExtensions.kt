// TARGET_BACKEND: JVM
// WITH_REFLECT
// LANGUAGE: +CompanionBlocks +CompanionExtensions

import kotlin.test.assertEquals
import kotlin.reflect.KCallable
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

private fun <T> KCallable<T>.callBy(vararg args: Any?): T =
    callBy(parameters.associateWith { args[it.index] })

fun box(): String {
    val f = A::class.members.single { it.name == "f" }
    assertEquals(Z("a"), f.callBy("a"))

    val p = A::class.members.single { it.name == "p" } as KMutableProperty0<Z>
    assertEquals(Z("p"), p.callBy())
    assertEquals(Unit, p.setter.callBy(Z("b")))
    assertEquals(Z("b"), p.getter.callBy())

    val g = A::g
    assertEquals(Z("c"), g.callBy("c"))

    val q = A::q
    assertEquals(Z("q"), q.callBy())
    assertEquals(Unit, q.setter.callBy(Z("d")))
    assertEquals(Z("d"), q.getter.callBy())

    return "OK"
}
