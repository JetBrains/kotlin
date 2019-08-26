// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
class Foo {
    fun bar(): String {
        fun <T> foo(t:() -> T) : T = t()
        foo { }
        return "OK"
    }
}

fun box(): String {
    return Foo().bar()
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ WASM_FUNCTION_REFERENCES_UNSUPPORTED
