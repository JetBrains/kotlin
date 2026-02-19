// similar to KT-83607, KT-83740
// WASM_CHECK_INSTRUCTION_NOT_IN_FUNCTION: instruction=br_table inFunction=box
// ISSUE: KT-83607 is fixed in 2.3.20-Beta2
// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_STAGE: WASM-JS:2.3.0

object State {
    var counter = 0
}

fun someFun(): String {
    State.counter++
    return "A"
}

fun box(): String {
    if (someFun() == "info") {
        return "Fail info"
    } else if (someFun() == "AAA") {
        return "Fail AAA"
    } else {
        if (State.counter != 2) {
            return "Fail counter"
        }
        return "OK"
    }
}