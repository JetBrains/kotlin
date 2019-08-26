// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
fun foo(f: (Int) -> Int) = f(0)

class Outer {
    class Nested {
        val y = foo { a -> a }
    }

    fun bar(): String {
        val a = Nested()
        return "OK"
    }
}

fun box() = Outer().bar()

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ WASM_FUNCTION_REFERENCES_UNSUPPORTED
