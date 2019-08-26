// IGNORE_BACKEND: WASM
// KJS_WITH_FULL_RUNTIME
fun typeName(a: Any?) : String {
    return when(a) {
        is ArrayList<*> -> "array list"
        else -> "no idea"
    }
}

fun box() : String {
    if(typeName(ArrayList<Int>()) != "array list") return "array list failed"
    return "OK"
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ ArrayList 
