// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

val xs = listOf<Any>().asSequence()

fun box(): String {
    val s = StringBuilder()
    for ((index, x) in xs.withIndex()) {
        return "Loop over empty list should not be executed"
    }
    return "OK"
}


// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: STDLIB_STRING_BUILDER
