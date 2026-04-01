// similar to KT-83607, KT-83740
// WASM_CHECK_INSTRUCTION_NOT_IN_FUNCTION: instruction=br_table inFunction=box
// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_STAGE: WASM-JS:2.3.0
// ^^^ K/Wasm backend v.2.3.0 has issue KT-83607, fixed only in 2.4.0-Beta1. So, a test `current frontend + 2.3.0 backend` expectedly fails

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