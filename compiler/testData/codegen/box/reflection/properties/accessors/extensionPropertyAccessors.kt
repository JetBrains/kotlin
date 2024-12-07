// TARGET_BACKEND: JVM

// WITH_REFLECT

import kotlin.test.assertEquals

var state: String = ""

var String.prop: String
    get() = length.toString()
    set(value) { state = this + value }

fun box(): String {
    val prop = String::prop

    assertEquals("3", prop.getter.invoke("abc"))
    assertEquals("5", prop.getter("defgh"))

    prop.setter("O", "K")

    return state
}
