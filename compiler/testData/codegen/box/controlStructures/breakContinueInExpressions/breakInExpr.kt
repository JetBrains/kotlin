// IGNORE_BACKEND: WASM
fun test(str: String): String {
    var s = ""
    for (i in 1..3) {
        s += if (i<2) str else break
    }
    return s
}

fun box(): String = test("OK")
// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ .. 
