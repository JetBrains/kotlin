// IGNORE_INLINER_K2: IR
// FILE: 1.kt

package x

operator fun String.provideDelegate(thisRef: Any?, prop: Any?) = this

operator fun String.getValue(thisRef: Any?, prop: Any?) = this

inline fun foo(): String {
    val x by "OK"
    return x
}

// FILE: 2.kt

import x.*

fun box(): String = foo()
