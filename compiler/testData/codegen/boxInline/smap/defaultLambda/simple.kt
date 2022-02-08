// SKIP_INLINE_CHECK_IN: inlineFun$default

// FILE: 1.kt


package test
inline fun inlineFun(capturedParam: String, lambda: () -> String = { capturedParam }): String {
    return lambda()
}

// FILE: 2.kt

import test.*

fun box(): String {
    return inlineFun("OK")
}
