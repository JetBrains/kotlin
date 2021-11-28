// WITH_REFLECT
// TARGET_BACKEND: JVM
// FILE: 1.kt

package test
inline val <reified T : Any> T.className: String; get() = T::class.java.simpleName

// FILE: 2.kt

import test.*

fun box(): String {
    val z = "OK".className
    if (z != "String") return "fail: $z"

    return "OK"
}
