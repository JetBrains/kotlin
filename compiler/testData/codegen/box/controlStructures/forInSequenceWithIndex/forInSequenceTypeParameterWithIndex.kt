// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: STDLIB_GENERATED
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

val xs = listOf("a", "b", "c", "d").asSequence()

fun <T : Sequence<*>> test(sequence: T): String {
    val s = StringBuilder()

    for ((index, x) in sequence.withIndex()) {
        s.append("$index:$x;")
    }

    return s.toString()
}

fun box(): String {
    val ss = test(xs)
    return if (ss == "0:a;1:b;2:c;3:d;") "OK" else "fail: '$ss'"
}