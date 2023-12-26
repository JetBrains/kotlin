// TARGET_BACKEND: JVM
// WITH_STDLIB
// JVM_ABI_K1_K2_DIFF: KT-62464

// FILE: 1.kt

package test

inline fun <R> call(crossinline s: () -> R) = { s() }.let { it() }

inline fun test(crossinline z: () -> String) = { z() }

// FILE: 2.kt

import test.*

fun box(): String {
    val res = call {
        test { "OK" }
    }

    // Check that Java reflection doesn't crash. Actual values are tested in bytecodeListing/inline/enclosingInfo/.
    res.javaClass.enclosingMethod
    res.javaClass.enclosingClass

    val res2 = call {
        call {
            test { "OK" }
        }
    }

    res2.javaClass.enclosingMethod
    res2.javaClass.enclosingClass

    return res2()
}
