// LANGUAGE: -AbstractClassMemberNotImplementedWithIntermediateAbstractClass
// IGNORE_BACKEND_K2: JVM_IR, JS_IR, JS_IR_ES6, NATIVE, WASM
// IGNORE_IR_DESERIALIZATION_TEST: NATIVE
// FIR status: don't support legacy feature
// IGNORE_IR_DESERIALIZATION_TEST: JS_IR
// ^^^ Source code is not compiled in JS.

// FILE: 1.kt
class Test: Impl(), CProvider

fun box() = "OK"

// FILE: 2.kt
open class C
class D: C()

interface CProvider {
    fun getC(): C
}

interface DProvider {
    fun getC(): D = D()
}

open class Impl: DProvider
