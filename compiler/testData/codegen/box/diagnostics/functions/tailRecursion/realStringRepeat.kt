// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: STDLIB_STRING_BUILDER
// KJS_WITH_FULL_RUNTIME
// DONT_RUN_GENERATED_CODE: JS

tailrec fun String.repeat(num : Int, acc : StringBuilder = StringBuilder()) : String =
        if (num == 0) acc.toString()
        else repeat(num - 1, acc.append(this))

fun box() : String {
    val s = "a".repeat(10000)
    return if (s.length == 10000) "OK" else "FAIL: ${s.length}"
}
