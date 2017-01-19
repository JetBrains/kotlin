// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_RUNTIME

import kotlin.reflect.KCallable
import kotlin.test.*

class M {
    fun foo() {}
    val bar = 1
}

fun checkEquals(x: KCallable<*>, y: KCallable<*>) {
    assertEquals(x, y)
    assertEquals(y, x)
    assertEquals(x.hashCode(), y.hashCode())
}

fun checkToString(x: KCallable<*>, expected: String) {
    assertEquals(expected + " (Kotlin reflection is not available)", x.toString())
}

fun box(): String {
    checkEquals(M::foo, M::foo)
    checkEquals(M::bar, M::bar)
    checkEquals(::M, ::M)

    checkToString(M::foo, "function foo")
    checkToString(M::bar, "property bar")
    checkToString(::M, "constructor")

    return "OK"
}
