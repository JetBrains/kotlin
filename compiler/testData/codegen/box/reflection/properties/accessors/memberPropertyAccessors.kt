// TARGET_BACKEND: JVM

// WITH_REFLECT

import kotlin.test.assertEquals

class C(var state: String)

fun box(): String {
    val prop = C::state

    val c = C("1")
    assertEquals("1", prop.getter.invoke(c))
    assertEquals("1", prop.getter(c))

    prop.setter(c, "OK")

    return prop.get(c)
}
