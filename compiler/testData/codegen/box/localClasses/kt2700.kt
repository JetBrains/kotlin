// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
package a.b

interface Test {
    fun invoke(): String {
        return "OK"
    }
}

private val a : Test = {
    object : Test {

    }
}()

fun box(): String {
    return a.invoke();
}
// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ WASM_FUNCTION_REFERENCES_UNSUPPORTED
