// IGNORE_INLINER_K2: IR
// FILE: 1.kt
// NO_SMAP_DUMP
package test

inline fun annotatedWith2(crossinline predicate: () -> Boolean)
    { val lambda = { any { predicate() } }; lambda() }


inline fun annotatedWith(crossinline predicate: () -> Boolean) =
        annotatedWith2 { predicate() }


inline fun any(s: () -> Boolean) {
    s()
}


// FILE: 2.kt
import test.*

fun box(): String {
    var result = "fail"

    annotatedWith { result = "OK"; true }

    return result
}


inline fun test(z: () -> Unit) {
    z()
}

