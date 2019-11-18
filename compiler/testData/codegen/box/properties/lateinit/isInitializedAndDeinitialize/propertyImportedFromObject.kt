// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME
// IGNORE_BACKEND: JS

package test

import test.Derived.p

open class Base(val b: Boolean)

object Derived : Base(::p.isInitialized) {
    lateinit var p: String
}

fun box(): String {
    return if (Derived.b) "Fail" else "OK"
}
