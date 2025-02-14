// WITH_STDLIB
// KJS_WITH_FULL_RUNTIME

// FILE: 1.kt
package test

inline fun getOrPutWeak(defaultValue: ()->String): String {
    val answer = defaultValue()
    return answer
}

// FILE: 2.kt
import test.*

fun box(): String {
    return getOrPutWeak { "OK" }
}
