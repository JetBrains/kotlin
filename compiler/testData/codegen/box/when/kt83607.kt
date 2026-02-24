// ISSUE: KT-83607
// WITH_STDLIB
// WASM_CHECK_INSTRUCTION_NOT_IN_FUNCTION: instruction=br_table inFunction=box
// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_STAGE: WASM-JS:2.3.0
// ^^^ K/Wasm backend v.2.3.0 has issue KT-83607, fixed only in 2.4.0-Beta1. So, a test `current frontend + 2.3.0 backend` expectedly fails

fun box(): String {
    val parent = "parent"
    val tag = "info"

    if (parent == "info") {
        return "FAIL 1"
    } else if (tag == "info") {
        return "OK"
    } else if (parent == "playerstats") {
        return "FAIL 2"
    } else {
        return "FAIL 3"
    }
    return "FAIL 4"
}
