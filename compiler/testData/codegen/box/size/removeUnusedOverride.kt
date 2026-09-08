// DONT_TARGET_EXACT_BACKEND: JVM, JVM_IR, NATIVE

// RUN_THIRD_PARTY_OPTIMIZER
// WASM_DCE_EXPECTED_OUTPUT_SIZE: wasm  34_473
// WASM_DCE_EXPECTED_OUTPUT_SIZE: mjs    6_140
// WASM_OPT_EXPECTED_OUTPUT_SIZE:          113

// ONLY_IR_DCE
// JS_DROP_REGION_COMMENTS
// JS_DCE_EXPECTED_OUTPUT_SIZE: JS_IR      4_692
// JS_DCE_EXPECTED_OUTPUT_SIZE: JS_IR_ES6  4_733

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
