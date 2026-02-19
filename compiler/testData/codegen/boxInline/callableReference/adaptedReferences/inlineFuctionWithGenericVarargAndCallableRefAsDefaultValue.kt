// WITH_STDLIB
// NO_CHECK_LAMBDA_INLINING

// FILE: 1.kt
package test

inline fun <reified T> runOnMultipleArgs(vararg a: T, ref: (Array<out T>) -> Unit = ::genericOf): Unit = ref(a)

fun <T> genericOf(vararg t: T) { }

// FILE: 2.kt
import test.*

fun box(): String {
    runOnMultipleArgs(1, 2, 3)
    return "OK"
}
