// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: STDLIB_COLLECTIONS
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
// IGNORE_BACKEND_FIR: JVM_IR

fun test() = uintArrayOf(1u).size

fun box(): String {
    val test = test()
    if (test != 1) return "Failed: $test"
    return "OK"
}