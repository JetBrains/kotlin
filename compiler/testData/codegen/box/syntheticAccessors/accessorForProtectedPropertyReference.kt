// IGNORE_BACKEND_FIR: JVM_IR
// FILE: 1.kt
package a

open class A {
    protected val v = "O"
    protected val w = "K"
}

// FILE: 2.kt
import a.*

class B : A() {
    fun foo() = ::v.get() + B::w.get(this)
}

fun box() = B().foo()
