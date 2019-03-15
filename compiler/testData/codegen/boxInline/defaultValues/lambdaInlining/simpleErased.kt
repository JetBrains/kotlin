// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt
// SKIP_INLINE_CHECK_IN: inlineFun$default
package test

inline fun inlineFun(capturedParam: String, lambda: () -> Any = { capturedParam as Any }): Any {
    return lambda()
}

// FILE: 2.kt
// CHECK_CONTAINS_NO_CALLS: box except=throwCCE;isType

import test.*

fun box(): String {
    return inlineFun("OK") as String
}
