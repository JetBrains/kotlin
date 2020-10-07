// IGNORE_BACKEND: JVM
// IGNORE_BACKEND_MULTI_MODULE: JVM, JVM_IR, JVM_MULTI_MODULE_IR_AGAINST_OLD, JVM_MULTI_MODULE_OLD_AGAINST_IR
// FILE: 1.kt
package test

interface A
class B : A

inline fun <T : A> foo(a: Any) = (a as? T != null).toString()[0]

// FILE: 2.kt

import test.*

fun box(): String {
    val s = "" + foo<B>(Any()) + foo<B>(object : A {}) + foo<B>(B())
    if (s != "ftt") return "fail: $s"
    return "OK"
}

