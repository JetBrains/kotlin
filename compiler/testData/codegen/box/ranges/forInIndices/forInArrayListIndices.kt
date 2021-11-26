// IGNORE_BACKEND: WASM
// WASM_MUTE_REASON: STDLIB_TEXT
// WITH_STDLIB

fun foo(): String {
    val a = ArrayList<String>()
    a.add("OK")
    for (i in a.indices) {
        return a[i]
    }
    return "Fail"
}

// KT-42642
fun bar(): String {
    val a = ArrayList<String>()
    a.add("O")
    a.add("K")
    val map = mutableMapOf<String, String>().apply {
        for (i in a.indices step 2) {
            put(a[i].toLowerCase(), a[i])
            put(a[i + 1].toLowerCase(), a[i + 1])
        }
    }
    return map.values.joinToString(separator = "")
}

fun box(): String {
    val r = foo()
    if (r != "OK") return r
    return bar()
}
