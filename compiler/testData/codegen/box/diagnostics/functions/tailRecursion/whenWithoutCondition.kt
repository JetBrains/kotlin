// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: IGNORED_IN_JS
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// TODO: muted automatically, investigate should it be ran for JS or not
// DONT_RUN_GENERATED_CODE: JS
// IGNORE_BACKEND: JS

tailrec fun withWhen2(counter : Int) : Int =
        when {
            counter == 0 -> counter
            counter == 50 -> 1 + <!NON_TAIL_RECURSIVE_CALL!>withWhen2<!>(counter - 1)
            <!NON_TAIL_RECURSIVE_CALL!>withWhen2<!>(0) == 0 -> withWhen2(counter - 1)
            else -> 1
        }

fun box() : String = if (withWhen2(100000) == 1) "OK" else "FAIL"