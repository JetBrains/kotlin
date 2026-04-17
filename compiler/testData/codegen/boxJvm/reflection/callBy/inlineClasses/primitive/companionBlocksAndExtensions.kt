// TARGET_BACKEND: JVM
// WITH_REFLECT
// LANGUAGE: +CompanionBlocksAndExtensions

import kotlin.test.assertEquals
import kotlin.reflect.KCallable
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

private fun <T> KCallable<T>.callBy(vararg args: Any?): T =
    callBy(parameters.associateWith { args[it.index] })

fun box(): String {
    val f = A::class.members.single { it.name == "f" }
    assertEquals(Z(1), f.callBy(1))

    val p = A::class.members.single { it.name == "p" } as KMutableProperty0<Z>
    assertEquals(Z(10), p.callBy())
    assertEquals(Unit, p.setter.callBy(Z(11)))
    assertEquals(Z(11), p.getter.callBy())

    val g = A::g
    assertEquals(Z(2), g.callBy(2))

    val q = A::q
    assertEquals(Z(20), q.callBy())
    assertEquals(Unit, q.setter.callBy(Z(21)))
    assertEquals(Z(21), q.getter.callBy())

    return "OK"
}
