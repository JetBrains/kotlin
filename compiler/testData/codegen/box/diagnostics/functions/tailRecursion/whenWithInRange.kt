// !DIAGNOSTICS: -UNUSED_PARAMETER
// TODO: muted automatically, investigate should it be ran for JS or not
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
