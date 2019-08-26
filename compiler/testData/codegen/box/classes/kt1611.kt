// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
fun box(): String {
    return Foo().doBar("OK")
}

class Foo() {
    val bar : (str : String) -> String = { it }

    fun doBar(str : String): String {
        return bar(str);
    }
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ WASM_FUNCTION_REFERENCES_UNSUPPORTED
