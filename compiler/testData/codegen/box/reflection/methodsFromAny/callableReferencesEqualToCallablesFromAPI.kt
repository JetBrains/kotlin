// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_REFLECT

import kotlin.reflect.*
import kotlin.test.assertEquals

class A {
    fun foo() = "foo"
    val bar = "bar"
}

fun checkEqual(x: Any, y: Any) {
    assertEquals(x, y)
    assertEquals(y, x)
    assertEquals(x.hashCode(), y.hashCode())
}

fun box(): String {
    checkEqual(A::foo, A::class.members.single { it.name == "foo" })
    checkEqual(A::bar, A::class.members.single { it.name == "bar" })

    return "OK"
}
