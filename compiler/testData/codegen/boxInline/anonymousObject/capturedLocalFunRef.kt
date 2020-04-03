// IGNORE_BACKEND: JVM
// IGNORE_BACKEND_MULTI_MODULE: JVM, JVM_IR
// FILE: 1.kt

package test

inline fun <T> myRun(block: () -> T) = block()

// FILE: 2.kt

import test.*

fun box(): String {
    val x = myRun {
        fun foo() = "OK"

        val o = object {
            val f = ::foo
            fun bar() = f()
        }
        o
    }
    return x.bar()
}