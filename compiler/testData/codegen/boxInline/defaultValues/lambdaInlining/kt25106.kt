// FILE: 1.kt
// SKIP_INLINE_CHECK_IN: inlineFun$default
package test

inline fun inlineFun(action: () -> Any = { "OK" }): Any {
    return action()
}

// FILE: 2.kt
import test.*

fun box(): String {
    return inlineFun() as String
}