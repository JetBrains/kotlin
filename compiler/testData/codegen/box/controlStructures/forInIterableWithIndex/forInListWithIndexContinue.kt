// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: STDLIB_GENERATED
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

fun test(xs: List<String>): String {
    var r = ""
    for ((i, x) in xs.withIndex()) {
        if (i % 2 == 0) continue
        r += "$i:$x;"
    }
    return r
}

fun box(): String {
    val t = test(listOf("a", "b", "c", "d", "e"))
    if (t != "1:b;3:d;") return "Failed: $t"
    return "OK"
}