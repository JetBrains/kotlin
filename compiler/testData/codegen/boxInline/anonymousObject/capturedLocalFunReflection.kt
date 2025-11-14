// ISSUE: KT-78537
// NO_CHECK_LAMBDA_INLINING
// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_STAGE: 2.2.0
// ^^^ KT-78537 is fixed in 2.3.0-Beta1

// FILE: 1.kt
package test

inline fun <T> myRun(block: () -> T) = block()

// FILE: 2.kt
import test.*

fun box(): String {
    val name = myRun {
        fun foo() = "fail 1"
        val fooRef = ::foo
        fooRef.name
    }
    return if (name == "foo") "OK" else name
}
