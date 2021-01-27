// SKIP_INLINE_CHECK_IN: inlineFun$default
// FILE: 1.kt
package test

class Item

inline fun inlineFun(number: String, getItem: ((String) -> String?) = { null }): String {
    return number + (getItem(number) ?: "")
}

// FILE: 2.kt
import test.*

fun box(): String {
    return inlineFun("OK")
}
