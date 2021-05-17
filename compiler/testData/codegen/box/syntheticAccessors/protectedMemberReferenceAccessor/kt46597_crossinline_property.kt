// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR

// FILE: a.kt
package a

abstract class Base {
    protected val property get() = "OK"
}

// FILE: b.kt
import a.Base

class SubClass : Base() {
    fun call() =
        higherOrder(::property)

    inline fun higherOrder(crossinline lambda: () -> String) =
        lambda()
}

fun box() = SubClass().call()
