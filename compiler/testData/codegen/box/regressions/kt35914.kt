// !LANGUAGE: +NewInference
// IGNORE_BACKEND: WASM
// WITH_RUNTIME
// KJS_WITH_FULL_RUNTIME
// IGNORE_BACKEND_FIR: JVM_IR

class Inv<T>
fun <T> bar(x: Inv<T>.() -> Unit) = x

fun box(): String {
    listOf(
        bar<Char> { },
        bar { } // the problem is here
    )
    return "OK"
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ listOf 
