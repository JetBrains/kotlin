// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

fun box(): String {
    var x: MutableCollection<Int> = ArrayList()
    x + ArrayList()
    return "OK"
}



// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: STDLIB_ARRAY_LIST
