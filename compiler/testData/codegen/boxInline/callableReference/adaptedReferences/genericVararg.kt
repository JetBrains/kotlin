// IGNORE_BACKEND: JVM_IR, JVM_IR_SERIALIZE
// WITH_STDLIB

// FILE: 1.kt
package test

inline fun <reified T, R> runOnMultipleArgs(vararg a: T, ref: (Array<out T>) -> R): R = ref(a)

fun <T> genericOf(vararg t: T) { }

// FILE: 2.kt
import test.*

fun box(): String {
    runOnMultipleArgs(1, 2, 3, ref = ::mutableListOf)
    runOnMultipleArgs(1, 2, 3, ref = ::genericOf)
    return "OK"
}
