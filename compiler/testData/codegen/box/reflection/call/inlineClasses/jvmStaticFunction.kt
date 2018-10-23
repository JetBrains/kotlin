// IGNORE_BACKEND: JS_IR, JS, NATIVE, JVM_IR
// JVM_TARGET: 1.8
// WITH_REFLECT

import kotlin.reflect.KFunction
import kotlin.test.assertEquals

inline class S(val value: String) {
    operator fun plus(other: S): S = S(this.value + other.value)
}

object C {
    @JvmStatic
    fun foo(x: S, y: String): S = x + S(y)
}

interface I {
    companion object {
        @JvmStatic
        fun bar(x: String, y: S): S = S(x) + y
    }
}

fun box(): String {
    assertEquals(S("ab"), C::foo.call(S("a"), "b"))
    assertEquals(S("cd"), (I)::bar.call("c", S("d")))

    val unboundFoo = C::class.members.single { it.name == "foo" } as KFunction<*>
    assertEquals(S("ef"), unboundFoo.call(C, S("e"), "f"))

    val unboundBar = I.Companion::class.members.single { it.name == "bar" } as KFunction<*>
    assertEquals(S("gh"), unboundBar.call(I, "g", S("h")))

    return "OK"
}
