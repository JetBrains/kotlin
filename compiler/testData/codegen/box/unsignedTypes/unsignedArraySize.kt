// IGNORE_BACKEND: WASM
// WASM_MUTE_REASON: UNSIGNED_ARRAYS
// WITH_RUNTIME

fun test() = uintArrayOf(1u).size

fun box(): String {
    val test = test()
    if (test != 1) return "Failed: $test"
    return "OK"
}