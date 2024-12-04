// CHECK_BYTECODE_LISTING
// FIR_IDENTICAL
// IGNORE_INLINER: IR

// FILE: 1.kt
package test

inline fun <R> myRun(g: () -> R) = g()

// FILE: 2.kt
import test.*

inline fun <reified A> Any?.complicatedCast() =
    myRun {                     // 1. Inline function call with a lambda
        object {                // 2. Anonymous object inside a lambda
            fun f(x: Any?): A { // 3. Anonymous object uses a reified type parameter
                val y = x       // 4. A method in that object uses locals
                return y as A
            }
        }.f(this)               // 5. The lambda captures more values than the object
    }

fun box() = "OK".complicatedCast<String>()
