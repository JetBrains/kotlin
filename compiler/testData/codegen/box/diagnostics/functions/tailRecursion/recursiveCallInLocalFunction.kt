// IGNORE_BACKEND: WASM
// WASM_MUTE_REASON: IGNORED_IN_JS

<!NO_TAIL_CALLS_FOUND!>tailrec<!> fun foo() {
    fun bar() {
        <!NON_TAIL_RECURSIVE_CALL!>foo<!>()
    }
}

fun box(): String {
    foo()
    return "OK"
}
