// TARGET_BACKEND: JVM
// WITH_STDLIB

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
