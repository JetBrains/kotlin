// WITH_STDLIB
// WASM_CHECK_INSTRUCTION_NOT_IN_FUNCTION: instruction=br_table inFunction=box

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
