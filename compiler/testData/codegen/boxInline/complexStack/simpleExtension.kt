// FILE: 1.kt

package test

inline fun foo(x: String) = x

inline fun processRecords(s: String?, block: String.(String) -> String) = s?.block(foo("O"))

// FILE: 2.kt

import test.*

fun test(): String? {
    return processRecords("O") { this + "K" }
}

fun box(): String = test()!!
