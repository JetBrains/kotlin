// NO_CHECK_LAMBDA_INLINING
// IGNORE_BACKEND: JVM
// IGNORE_BACKEND_MULTI_MODULE: JVM, JVM_MULTI_MODULE_OLD_AGAINST_IR
// FILE: 1.kt

package test

open class A

inline fun <T> call(lambda: () -> T): T {
    return lambda()
}

// FILE: 2.kt

import test.*

fun box(): String {
    val x = "OK"
    val result = call {
        object : A() {
            val p = x
        }
    }

    return result.p
}
