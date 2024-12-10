// LANGUAGE: -ProhibitDataClassesOverridingCopy
// IGNORE_BACKEND_K1: NATIVE
// IGNORE_BACKEND_K2: ANY
// IGNORE_IR_DESERIALIZATION_TEST: NATIVE
// FIR status: Disabling ProhibitDataClassesOverridingCopy is not supported
// IGNORE_IR_DESERIALIZATION_TEST: JS_IR
// ^^^ Source code is not compiled in JS.

fun box(): String {
    val a: A = B(1)
    a.copy(1)
    a.component1()
    return "OK"
}

interface A {
    fun copy(x: Int): A
    fun component1(): Any
}

data class B(val x: Int) : A
