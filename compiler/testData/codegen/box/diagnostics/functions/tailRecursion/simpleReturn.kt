// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: IGNORED_IN_JS
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// TODO: muted automatically, investigate should it be ran for JS or not
// DONT_RUN_GENERATED_CODE: JS
// IGNORE_BACKEND: JS

tailrec fun test(x : Int) : Int {
    if (x == 10) {
        return 1 + <!NON_TAIL_RECURSIVE_CALL!>test<!>(x - 1)
    }
    if (x > 0) {
        return test(x - 1)
    }
    return 0
}

fun box() : String = if (test(1000000) == 1) "OK" else "FAIL"