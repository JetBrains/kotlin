// KJS_WITH_FULL_RUNTIME
fun box(): String {
    val list = ArrayList<String>()
    list.add("0")
    list[0][0]
    list[0].length
    return "OK"
}



// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: STDLIB_ARRAY_LIST
