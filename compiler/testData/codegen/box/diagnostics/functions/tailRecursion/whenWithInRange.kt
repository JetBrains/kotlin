// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: IGNORED_IN_JS
// !DIAGNOSTICS: -UNUSED_PARAMETER
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6

// TODO: muted automatically, investigate should it be ran for JS or not
// DONT_RUN_GENERATED_CODE: JS
// IGNORE_BACKEND: JS

tailrec fun withWhen(counter : Int, d : Any) : Int =
        when (counter) {
            0 -> counter
            1, 2 -> withWhen(counter - 1, "1,2")
            in 3..49 -> withWhen(counter - 1, "3..49")
            50 -> 1 + <!NON_TAIL_RECURSIVE_CALL!>withWhen<!>(counter - 1, "50")
            !in 0..50 -> withWhen(counter - 1, "!0..50")
            else -> withWhen(counter - 1, "else")
        }

fun box() : String = if (withWhen(100000, "test") == 1) "OK" else "FAIL"