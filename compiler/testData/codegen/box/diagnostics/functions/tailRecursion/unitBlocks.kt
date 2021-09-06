// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: IGNORED_IN_JS
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// TODO: muted automatically, investigate should it be ran for JS or not
// DONT_RUN_GENERATED_CODE: JS
// IGNORE_BACKEND: JS

tailrec fun test(x : Int) : Unit {
    if (x > 800000) {
        test(x - 1)
    } else if (x > 600000) {
        test(x - 1)
        return
    } else if (x > 400000) {
        <!NON_TAIL_RECURSIVE_CALL!>test<!>(1)
        if (x > 200000) {
            test(x - 1)
        }
        return
    } else if (x > 0) {
        test(x - 1)
    }
}

fun box() : String {
    test(1000000)
    return "OK"
}
