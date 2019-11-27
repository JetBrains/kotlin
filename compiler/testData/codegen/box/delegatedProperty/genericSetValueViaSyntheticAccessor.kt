// IGNORE_BACKEND_FIR: JVM_IR
// FILE: Var.kt
package pvar

open class PVar<T>(private var value: T) {
    protected operator fun getValue(thisRef: Any?, prop: Any?) = value

    protected operator fun setValue(thisRef: Any?, prop: Any?, newValue: T) {
        value = newValue
    }
}

// FILE: test.kt
import pvar.*

class C : PVar<Long>(42L) {
    inner class Inner {
        var x by this@C
    }
}

fun box(): String {
    val inner = C().Inner()
    inner.x = 1L
    return "OK"
}