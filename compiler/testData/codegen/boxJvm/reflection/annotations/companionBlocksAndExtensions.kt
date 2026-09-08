// TARGET_BACKEND: JVM
// WITH_REFLECT
// LANGUAGE: +CompanionBlocks +CompanionExtensions

import kotlin.reflect.KCallable
import kotlin.test.assertEquals

annotation class Anno(val value: String)

class A {
    companion {
        @Anno("f")
        fun f(x: Int): Int = x + 1
        @Anno("p")
        var p: String = "a"
            @Anno("<get-p>")
            get
            @Anno("<set-p>")
            set
    }
}

@Anno("g")
companion fun A.g(x: Int): Int = x + 10

private var storage = "b"
@Anno("q")
companion var A.q: String
    @Anno("<get-q>")
    get() = storage
    @Anno("<set-q>")
    set(value) { storage = value }

private fun check(c: KCallable<*>) {
    val anno = c.annotations.singleOrNull() as? Anno
    if (anno == null) throw AssertionError("No single annotation found for $c: ${c.annotations}")
    assertEquals(c.name, anno.value)
}

fun box(): String {
    check(A::f)
    check(A::class.members.single { it.name == "f" })

    check(A::p)
    check(A::class.members.single { it.name == "p" })
    check(A::p.getter)
    check(A::p.setter)

    check(A::g)
    check(A::q)
    check(A::q.getter)
    check(A::q.setter)

    return "OK"
}
