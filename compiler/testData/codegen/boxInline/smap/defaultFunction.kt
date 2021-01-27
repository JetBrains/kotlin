// NO_CHECK_LAMBDA_INLINING

// FILE: 1.kt

package test
inline fun inlineFun(capturedParam: String, noinline lambda: () -> String = { capturedParam }): String {
    return lambda()
}

// FILE: 2.kt
// NO_SMAP_DUMP
import test.*

fun box(): String {
    return inlineFun("OK")
}

