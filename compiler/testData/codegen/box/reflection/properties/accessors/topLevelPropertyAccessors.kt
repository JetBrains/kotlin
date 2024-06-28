// TARGET_BACKEND: JVM

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
