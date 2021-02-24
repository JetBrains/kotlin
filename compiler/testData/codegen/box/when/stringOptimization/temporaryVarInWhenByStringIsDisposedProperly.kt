// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: STDLIB_GENERATED
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

val String.name get() = this

fun List<String>.normalize(): List<String> {
    val list = ArrayList<String>()
    for (str in this) {
        when (str.name) {
            "." -> {}
            ".." -> if (!list.isEmpty() && list.last().name != "..") list.removeAt(list.size - 1) else list.add(str)
            else -> list.add(str)
        }
    }
    return list
}

fun box(): String {
    val xs = listOf("a", "b", ".", "..").normalize()
    if (xs != listOf("a")) return "Fail: $xs"

    return "OK"
}