// IGNORE_BACKEND: WASM
fun test(x: Int): String = when(x) {
        0 -> "zero"
        1 -> "one"
        2 -> "two"
        else -> blowUpHorribly()
    }

fun blowUpHorribly(): Nothing = throw RuntimeException("Blow up!")

fun box(): String {
    test(1)
    return "OK"
}
// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ RuntimeException 
