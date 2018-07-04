// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt
package test

inline fun inline1(crossinline action: () -> Unit) {
    { action () }
    action();
}

inline fun inline2(crossinline action: () -> Unit) = { action() }


// FILE: 2.kt
import test.*

var result = "fail"
fun box(): String {
    inline1 { inline2 { result ="OK" }() }

    return result
}

