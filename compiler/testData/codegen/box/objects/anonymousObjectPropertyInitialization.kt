// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
interface T {
    fun foo(): String
}

val o = object : T {
    val a = "OK"
    val f = {
        a
    }()

    override fun foo() = f
}

fun box() = o.foo()

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ WASM_FUNCTION_REFERENCES_UNSUPPORTED
