// TARGET_BACKEND: JVM
// WITH_RUNTIME

// IGNORE_BACKEND_FIR: JVM_IR
//  - FIR2IR should generate call to fake override

// FILE: a.kt
package a

abstract class Base {
    @JvmField
    protected val property = "OK"
}

// FILE: b.kt
import a.Base

class SubClass : Base() {
    fun call() = ::property
}

fun box() = SubClass().call().invoke()
