// IGNORE_BACKEND_K2: JVM_IR, JS_IR, JS_IR_ES6, NATIVE, WASM
// IGNORE_BACKEND_K2_MULTI_MODULE: JVM_IR JVM_IR_SERIALIZE NATIVE
// !LANGUAGE: -ProhibitSuperCallsFromPublicInline
// FILE: 1.kt

package test

open class A {
    open fun test() = "OK"
}

object X : A() {
    override fun test(): String {
        return "fail"
    }

    inline fun doTest(): String {
        return super.test()
    }
}

// FILE: 2.kt

import test.*

fun box(): String {
    return X.doTest()
}
