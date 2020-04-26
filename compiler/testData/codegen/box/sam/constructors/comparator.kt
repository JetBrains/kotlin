// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME
// KJS_WITH_FULL_RUNTIME

fun box(): String {
    val list = mutableListOf(3, 2, 4, 8, 1, 5)
    val expected = listOf(8, 5, 4, 3, 2, 1)
    list.sortWith(Comparator { a, b -> b - a })
    return if (list == expected) "OK" else list.toString()
}


// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: STDLIB_ARRAY_LIST
