// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

val xs = listOf("a", "b", "c", "d").asSequence()

fun box(): String {
    val s = StringBuilder()

    for ((index, x) in xs.withIndex()) {
        if (index == 0) continue
        if (index == 3) break
        s.append("$index:$x;")
    }

    val ss = s.toString()
    return if (ss == "1:b;2:c;") "OK" else "fail: '$ss'"
}


// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: STDLIB_GENERATED
