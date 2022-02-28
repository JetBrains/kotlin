// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND_FIR_MULTI_MODULE: JVM_IR JVM_IR_SERIALIZE
// !LANGUAGE: -ProhibitSuperCallsFromPublicInline
// FILE: 1.kt

package test

open class A {
    open fun test(s: String) = s
}

object B : A() {
    override fun test(s: String) = "fail"

    inline fun doTest(s: String) = super.test(s)
}

object C : A() {
    override fun test(s: String) = "fail"

    inline fun doTest(s: String) = super.test(s)
}

// FILE: 2.kt

import test.*

fun box(): String {
    return B.doTest("O") + C.doTest("K")
}
