// TARGET_BACKEND: JVM
// NO_CHECK_LAMBDA_INLINING
// WITH_RUNTIME
// IGNORE_BACKEND: JVM
// IGNORE_BACKEND_MULTI_MODULE: JVM, JVM_MULTI_MODULE_OLD_AGAINST_IR
// FILE: 1.kt

package test

inline fun <reified T : Any> foo(crossinline function: () -> T) {
    T::class.java.name

    object {
        fun bar() {
            function()
        }

        init {
            bar()
        }
    }
}

// FILE: 2.kt

import test.*

fun box(): String {
    var result = "Fail"
    foo {
        object {
            init {
                result = "OK"
            }
        }
    }
    return result
}
