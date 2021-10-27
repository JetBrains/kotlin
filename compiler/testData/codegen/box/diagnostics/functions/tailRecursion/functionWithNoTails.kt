// IGNORE_BACKEND: WASM
// WASM_MUTE_REASON: IGNORED_IN_JS
// IGNORE_BACKEND: JS_IR_ES6

<!NO_TAIL_CALLS_FOUND!>tailrec<!> fun noTails() {
    // nothing here
}

fun box(): String {
    noTails()
    return "OK"
}
