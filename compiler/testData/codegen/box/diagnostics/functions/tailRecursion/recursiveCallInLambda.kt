// IGNORE_BACKEND: WASM
// WASM_MUTE_REASON: IGNORED_IN_JS
// !DIAGNOSTICS: -UNUSED_PARAMETER
// IGNORE_BACKEND: JS_IR_ES6

<!NO_TAIL_CALLS_FOUND!>tailrec<!> fun foo() {
    bar {
        <!NON_TAIL_RECURSIVE_CALL!>foo<!>()
    }
}

fun bar(a: Any) {}

fun box(): String {
    foo()
    return "OK"
}
