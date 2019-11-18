// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_REFLECT

import kotlin.test.assertEquals

var state: String = ""

fun box(): String {
    val prop = ::state

    assertEquals("", prop.getter.invoke())
    assertEquals("", prop.getter())

    prop.setter("OK")

    return prop.get()
}
