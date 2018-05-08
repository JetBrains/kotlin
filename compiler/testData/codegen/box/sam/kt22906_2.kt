// TARGET_BACKEND: JVM
// WITH_RUNTIME
// FILE: kt22906_1.kt
package test

class C {
    fun startTemplate(): String {
        val y = object {
            fun foo(): String {
                val job = { "OK" }
                return java.util.concurrent.Callable(job).call()
            }

        }
        return y.foo()
    }

    fun foo() {
        val y = object {
            fun foo(): String {
                val job = { "OK2" }
                return java.util.concurrent.Callable(job).call()
            }

        }
    }
}

// FILE: kt22906_2.kt
import test.*

fun box(): String {
    if (java.lang.Class.forName("test.C\$sam\$java_util_concurrent_Callable\$0") == null) return "fail: can't find sam wrapper"

    return C().startTemplate()
}