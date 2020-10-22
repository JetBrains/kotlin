// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: IGNORED_IN_JS
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// TODO: muted automatically, investigate should it be ran for JS or not
// DONT_RUN_GENERATED_CODE: JS
// IGNORE_BACKEND: JS

tailrec fun badTails(x : Int) : Int {
    if (x < 50 && x != 10 && x > 0) {
        return 1 + <!NON_TAIL_RECURSIVE_CALL!>badTails<!>(x - 1)
    }
    else if (x == 10) {
        @Suppress("NON_TAIL_RECURSIVE_CALL")
        return 1 + badTails(x - 1)
    } else if (x >= 50) {
        return badTails(x - 1)
    }
    return 0
}

fun box(): String {
    badTails(1000000)
    return "OK"
}