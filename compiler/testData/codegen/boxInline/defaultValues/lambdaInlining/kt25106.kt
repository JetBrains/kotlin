// SKIP_INLINE_CHECK_IN: inlineFun$default
// FILE: 1.kt
package test

inline fun inlineFun(action: () -> Any = { "OK" }): Any {
    return action()
}

// FILE: 2.kt
import test.*

fun box(): String {
    return inlineFun() as String
}
