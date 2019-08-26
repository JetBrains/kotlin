// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
fun foo() {
    val array = Array(0, { IntArray(0) } )
    array.forEach { println(it.asList()) }
}

fun box(): String {
    foo() // just to be sure, that no exception happens
    return "OK"
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ forEach 
