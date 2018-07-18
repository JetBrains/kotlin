// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME
// WITH_REFLECT
// FILE: 1.kt
package test

inline val <reified T: Any> T.value: String
    get() = T::class.simpleName!!

// FILE: 2.kt
import test.*

class OK

fun box(): String {
    return OK().value ?: "fail"
}