// IGNORE_BACKEND: JS_IR, JS, NATIVE, JVM_IR
// WITH_REFLECT

import kotlin.reflect.KCallable
import kotlin.reflect.jvm.isAccessible
import kotlin.test.assertEquals

inline class S(val value: String) {
    operator fun plus(other: S): S = S(this.value + other.value)
}

class C {
    private var member: S = S("")

    fun unboundRef() = C::member.apply { isAccessible = true }
    fun boundRef() = this::member.apply { isAccessible = true }
}

private var topLevel: S = S("")

fun box(): String {
    val c = C()
    assertEquals(Unit, c.unboundRef().setter.call(c, S("ab")))
    assertEquals(S("ab"), c.unboundRef().call(c))
    assertEquals(S("ab"), c.unboundRef().getter.call(c))

    assertEquals(Unit, c.boundRef().setter.call(S("cd")))
    assertEquals(S("cd"), c.boundRef().call())
    assertEquals(S("cd"), c.boundRef().getter.call())

    val topLevel = ::topLevel.apply { isAccessible = true }
    assertEquals(Unit, topLevel.setter.call(S("ef")))
    assertEquals(S("ef"), topLevel.call())
    assertEquals(S("ef"), topLevel.getter.call())

    return "OK"
}
