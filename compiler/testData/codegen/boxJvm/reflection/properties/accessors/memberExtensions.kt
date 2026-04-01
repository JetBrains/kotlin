// TARGET_BACKEND: JVM

// WITH_REFLECT

import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.test.assertEquals

class C(var state: String) {
    var String.prop: String
        get() = length.toString()
        set(value) { state = this + value }
}

fun box(): String {
    val prop = C::class.memberExtensionProperties.single() as KMutableProperty2<C, String, String>

    val c = C("")
    assertEquals("3", prop.getter.invoke(c, "abc"))
    assertEquals("1", prop.getter(c, "d"))

    prop.setter(c, "O", "K")

    return c.state
}
