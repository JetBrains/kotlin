// !LANGUAGE: +ProperVisibilityForCompanionObjectInstanceField
// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR

class Outer {
    private companion object {
        val result = "OK"
    }

    class Nested {
        fun foo() = { result }()
    }

    fun test() = Nested().foo()
}

fun box() = Outer().test()
// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ WASM_FUNCTION_REFERENCES_UNSUPPORTED
