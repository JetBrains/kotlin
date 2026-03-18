// LANGUAGE: +ContextParameters
// OPT_IN: kotlin.ExperimentalContextParameters
// TARGET_BACKEND: JVM
// IGNORE_BACKEND_K1: ANY
// WITH_REFLECT

package test

import kotlin.reflect.KMutableProperty
import kotlin.test.assertEquals

interface A
interface B

class C {
    context(a: A, _: B) fun f(z: Any) {}
    context(_: A, b: B) var p: Any?
        get() = null
        set(value) {}
}

fun box(): String {
    val f = C::class.members.single { it.name == "f" }
    assertEquals("context(a: test.A, _: test.B) fun test.C.f(kotlin.Any): kotlin.Unit", f.toString())

    val p = C::class.members.single { it.name == "p" } as KMutableProperty<*>
    assertEquals("context(_: test.A, b: test.B) var test.C.p: kotlin.Any?", p.toString())
    assertEquals("getter of context(_: test.A, b: test.B) var test.C.p: kotlin.Any?", p.getter.toString())
    assertEquals("setter of context(_: test.A, b: test.B) var test.C.p: kotlin.Any?", p.setter!!.toString())

    return "OK"
}
