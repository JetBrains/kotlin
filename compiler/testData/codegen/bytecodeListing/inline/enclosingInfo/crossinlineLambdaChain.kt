// JVM_ABI_K1_K2_DIFF: KT-62464

// FILE: 1.kt

package test

inline fun <R> call(crossinline s: () -> R) = { s() }.let { it() }

inline fun test(crossinline z: () -> String) = { z() }

// FILE: 2.kt

import test.*

fun box() {
    val res = call {
        test { "OK" }
    }

    val res2 = call {
        call {
            test { "OK" }
        }
    }
}
