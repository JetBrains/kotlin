// IGNORE_BACKEND: JS_IR, JS, NATIVE
// WITH_REFLECT

import kotlin.test.assertEquals

inline class S(val value: String) {
    operator fun plus(other: S): S = S(this.value + other.value)
}

class C {
    fun member(x: S, y: String): S = x + S(y)
}

fun topLevel(x: String, y: S): S = S(x) + y

fun S.extension(y: S): S = this + y

fun S.extension2(): String = value

fun box(): String {
    assertEquals(S("ab"), C::member.call(C(), S("a"), "b"))
    assertEquals(S("cd"), ::topLevel.call("c", S("d")))
    assertEquals(S("gh"), S::extension.call(S("g"), S("h")))
    assertEquals("_", S::extension2.call(S("_")))

    assertEquals(S("ij"), C()::member.call(S("i"), "j"))
    assertEquals(S("mn"), S("m")::extension.call(S("n")))
    assertEquals("_", S("_")::extension2.call())

    return "OK"
}
