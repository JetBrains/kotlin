// DONT_TARGET_EXACT_BACKEND: JVM, JVM_IR, NATIVE

// RUN_THIRD_PARTY_OPTIMIZER
// WASM_DCE_EXPECTED_OUTPUT_SIZE: wasm  28_610
// WASM_DCE_EXPECTED_OUTPUT_SIZE: mjs    6_287
// WASM_OPT_EXPECTED_OUTPUT_SIZE:          124

// ONLY_IR_DCE
// JS_DCE_EXPECTED_OUTPUT_SIZE: JS_IR      5_999
// JS_DCE_EXPECTED_OUTPUT_SIZE: JS_IR_ES6  6_113

interface I {
    fun foo() = "OK"
}

abstract class A : I

class B : A()

class C : A() {
    override fun foo(): String {
        return "C::foo"
    }
}

fun box() = B().foo()
