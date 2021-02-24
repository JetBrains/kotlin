// SKIP_INLINE_CHECK_IN: inlineFun$default
// FILE: 1.kt
package test

inline fun inlineFun(lambda: () -> Any = { "OK" as Any }): Any {
    return lambda()
}

// FILE: 2.kt
// CHECK_CONTAINS_NO_CALLS: box except=throwCCE;isType

import test.*

fun box(): String {
    return inlineFun() as String
}
