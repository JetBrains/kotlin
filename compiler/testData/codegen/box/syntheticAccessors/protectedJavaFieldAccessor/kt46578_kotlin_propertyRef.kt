// TARGET_BACKEND: JVM
// WITH_STDLIB

// FILE: kt46578_kotlin_propertyRef.kt
import p.*

class Derived : Base() {
    init {
        pf = "OK"
    }
    val ref = ::pf
}

fun box(): String {
    return Derived().ref.get()
}

// FILE: p/Base.kt
package p

open class Base {
    protected var pf = ""
}
