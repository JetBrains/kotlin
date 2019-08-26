// IGNORE_BACKEND: WASM
// DONT_RUN_GENERATED_CODE: JS

tailrec fun test(x : Int = 0, e : Any = "a") {
    if (x < 100000 && !e.equals("a")) {
        throw IllegalArgumentException()
    }
    if (x > 0) {
        test(x - 1)
    }
}

fun box() : String {
    test(100000, "b")
    return "OK"
}
// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ IllegalArgumentException 
