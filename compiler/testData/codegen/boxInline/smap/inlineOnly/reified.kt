// WITH_REFLECT
// TARGET_BACKEND: JVM
// FILE: 1.kt
package test

inline fun <reified T : Any> className() =  T::class.java.simpleName

// FILE: 2.kt

import test.*

fun box(): String {
    val z = className<String>()
    if (z != "String") return "fail: $z"

    return "OK"
}
