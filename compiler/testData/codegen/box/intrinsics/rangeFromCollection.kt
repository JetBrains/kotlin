// KJS_WITH_FULL_RUNTIME
fun box(): String {
    val list = ArrayList<IntRange>()
    list.add(1..3)
    list[0].start
    return "OK"
}



// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: STDLIB_ARRAY_LIST
