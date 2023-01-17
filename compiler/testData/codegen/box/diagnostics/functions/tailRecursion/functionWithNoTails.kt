// IGNORE_BACKEND: WASM
// WASM_MUTE_REASON: IGNORED_IN_JS

<!NO_TAIL_CALLS_FOUND!>tailrec<!> fun noTails() {
    // nothing here
}

fun box(): String {
    noTails()
    return "OK"
}
