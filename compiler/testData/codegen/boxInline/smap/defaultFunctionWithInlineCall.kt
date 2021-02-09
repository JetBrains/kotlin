// FILE: 1.kt
package test

inline fun inlineFun2(param: String): String {
    return param
}

inline fun inlineFun(param: String = "OK"): String {
    // The Kotlin stratum should only contain 1 out-of-range line, and
    // KotlinDebug should point it to this line:
    return inlineFun2(param)
}

// FILE: 2.kt
import test.*

fun box(): String {
    return inlineFun()
}

