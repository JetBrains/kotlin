// TARGET_BACKEND: JS_IR
// TARGET_BACKEND: WASM

// WASM_DCE_EXPECTED_OUTPUT_SIZE: wasm 13_506

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
