// IGNORE_BACKEND: JVM, JVM_IR
// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND_MULTI_MODULE: JVM, JVM_IR, JVM_MULTI_MODULE_OLD_AGAINST_IR, JVM_MULTI_MODULE_IR_AGAINST_OLD
// IGNORE_BACKEND_FIR_MULTI_MODULE: JVM_IR JVM_IR_SERIALIZE
// FILE: 1.kt
package test

interface I {
    fun f(): String
}

inline fun test(crossinline h: () -> String) = object : I {
    // TODO: this does not work because the inliner is not correctly remapping the capture.
    override fun f(): String = g()
    // TODO: and this does not work in JVM_IR because there is a redundant accessor.
    inline fun g(): String = h()
}

// FILE: 2.kt

import test.*

fun box(): String = test { "OK" }.f()
