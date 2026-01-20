// similar to KT-83607, KT-83740
// WASM_CHECK_INSTRUCTION_NOT_IN_SCOPE: instruction=br_table scope_function=box

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