// IGNORE_BACKEND_FIR: JVM_IR
// FILE: 1.kt
package test

inline fun myRun(f: () -> Nothing): Nothing = f()

// FILE: 2.kt
import test.*

interface I {
    val ok: String
        get() { myRun { return "OK" } }
}

class C : I

fun box(): String = C().ok
