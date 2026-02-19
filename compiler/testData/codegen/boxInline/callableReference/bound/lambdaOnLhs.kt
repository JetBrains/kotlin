// NO_CHECK_LAMBDA_INLINING
// KT-28042
// FILE: 1.kt

package test

fun <T> supplier(f: () -> T) = f

inline fun consumer1(c: (Unit) -> Unit) = c(Unit)

// FILE: 2.kt

import test.*

class A {
    fun f() {
        consumer1 {
            supplier {
                consumer1(consumer2())
            }::apply
        }
    }
    fun consumer2(): (Unit) -> Unit = {}
}

fun box(): String {
    A().f()
    return "OK"
}
