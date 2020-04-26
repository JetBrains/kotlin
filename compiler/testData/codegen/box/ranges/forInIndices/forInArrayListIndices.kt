// WITH_RUNTIME
// KJS_WITH_FULL_RUNTIME

fun box(): String {
    val a = ArrayList<String>()
    a.add("OK")
    for (i in a.indices) {
        return a[i]
    }
    return "Fail"
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: STDLIB_ARRAY_LIST
