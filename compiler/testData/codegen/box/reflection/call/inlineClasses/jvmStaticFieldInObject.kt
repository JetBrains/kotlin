// IGNORE_BACKEND: JS_IR, JS, NATIVE, JVM_IR
// JVM_TARGET: 1.8
// WITH_REFLECT

import kotlin.reflect.KMutableProperty1
import kotlin.reflect.jvm.isAccessible
import kotlin.test.assertEquals

inline class S(val value: String) {
    operator fun plus(other: S): S = S(this.value + other.value)
}

object C {
    @JvmStatic
    private var p: S = S("")

    fun boundRef() = this::p.apply { isAccessible = true }
}

fun box(): String {
    val unboundRef = C::class.members.single { it.name == "p" } as KMutableProperty1<C, S>
    unboundRef.isAccessible = true
    assertEquals(Unit, unboundRef.setter.call(C, S("ab")))
    assertEquals(S("ab"), unboundRef.call(C))
    assertEquals(S("ab"), unboundRef.getter.call(C))

    val boundRef = C.boundRef()
    assertEquals(Unit, boundRef.setter.call(S("cd")))
    assertEquals(S("cd"), boundRef.call())
    assertEquals(S("cd"), boundRef.getter.call())

    return "OK"
}
