// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
interface T

object Foo {
    private fun foo(p: T) = p

    private val v: Int = {
        val x = foo(O)
        42
    }()

    private object O : T

    val result = v
}

fun box(): String {
    val foo = Foo
    if (foo.result != 42) return "Fail: ${foo.result}"
    return "OK"
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ WASM_FUNCTION_REFERENCES_UNSUPPORTED
