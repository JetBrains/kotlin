// WITH_STDLIB
// NO_CHECK_LAMBDA_INLINING

// FILE: lib.kt

inline fun inlineFun(param: String, lambda: (String) -> String = { it }): String {
    return lambda(param)
}

// FILE: main.kt
import kotlin.test.*

fun box(): String {
    return inlineFun("OK")
}
