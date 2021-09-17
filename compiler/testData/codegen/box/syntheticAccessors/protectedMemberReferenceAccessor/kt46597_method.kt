// FILE: a.kt
package a

abstract class Base {
    protected fun method() = "OK"
}

// FILE: b.kt
import a.Base

class SubClass : Base() {
    fun call() = ::method
}

fun box() = SubClass().call().invoke()
