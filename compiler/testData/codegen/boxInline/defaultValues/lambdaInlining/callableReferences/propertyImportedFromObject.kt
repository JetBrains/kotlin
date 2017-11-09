// FILE: 1.kt
package test

object A {
    val ok = "OK"
}
inline fun stub() {}


// FILE: 2.kt
// LANGUAGE_VERSION: 1.2
// SKIP_INLINE_CHECK_IN: inlineFun$default
import test.A.ok

inline fun inlineFun(lambda: () -> String = ::ok): String {
    return lambda()
}

fun box(): String {
    return inlineFun()
}