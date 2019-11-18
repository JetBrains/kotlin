// IGNORE_BACKEND_FIR: JVM_IR
// FILE: test.kt
import base.*

class Derived : Base<Long>() {
    inner class Inner {
        fun foo() = this@Derived[0L]
    }
}

fun box() = Derived().Inner().foo()

// FILE: Base.kt
package base

open class Base<K> {
    protected operator fun get(key: K) = "OK"
}