// TARGET_BACKEND: JVM
// WITH_STDLIB

// FILE: kt46578_kotlin_delegated.kt
import p.*

class Derived : Base() {
    init {
        pf = "OK"
    }
    val delegated by ::pf
}

fun box(): String {
    return Derived().delegated
}

// FILE: p/Base.kt
package p

open class Base {
    protected var pf = ""
}
