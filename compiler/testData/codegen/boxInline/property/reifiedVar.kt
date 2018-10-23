// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME
// WITH_REFLECT
// FILE: 1.kt
package test

var bvalue: String = ""

inline var <reified T : Any> T.value: String
    get() = T::class.simpleName!! + bvalue
    set(p: String) {
        bvalue = p
    }

// FILE: 2.kt
import test.*

class O

fun box(): String {
    val o = O()
    val value1 = o.value
    if (value1 != "O") return "fail 1: $value1"

    o.value = "K"
    return o.value
}