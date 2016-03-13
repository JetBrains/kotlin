// FILE: 1.kt

package test

inline fun foo(x: String) = x

inline fun processRecords(block: (String) -> String) = block(foo("O"))

// FILE: 2.kt

import test.*

fun box(): String {
    return processRecords { ext -> ext + "K" }
}
