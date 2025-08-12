// NO_CHECK_LAMBDA_INLINING

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
