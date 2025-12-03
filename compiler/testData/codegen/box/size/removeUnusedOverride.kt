// TARGET_BACKEND: JS_IR
// TARGET_BACKEND: WASM

// RUN_THIRD_PARTY_OPTIMIZER
// WASM_DCE_EXPECTED_OUTPUT_SIZE: wasm  35_764
// WASM_DCE_EXPECTED_OUTPUT_SIZE: mjs    7_157
// WASM_OPT_EXPECTED_OUTPUT_SIZE:          148

// IGNORE_BACKEND: WASM_WASI

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
