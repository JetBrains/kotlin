// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: IGNORED_IN_JS
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// TODO: muted automatically, investigate should it be ran for JS or not
// DONT_RUN_GENERATED_CODE: JS
// IGNORE_BACKEND: JS

tailrec fun withWhen(counter : Int) : Int =
        when (counter) {
            0 -> counter
            50 -> 1 + <!NON_TAIL_RECURSIVE_CALL!>withWhen<!>(counter - 1)
            else -> withWhen(counter - 1)
        }

fun box() : String = if (withWhen(100000) == 1) "OK" else "FAIL"