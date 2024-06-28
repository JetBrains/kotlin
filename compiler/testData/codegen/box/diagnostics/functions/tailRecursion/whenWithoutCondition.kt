// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

tailrec fun withWhen2(counter : Int) : Int =
        when {
            counter == 0 -> counter
            counter == 50 -> 1 + <!NON_TAIL_RECURSIVE_CALL!>withWhen2<!>(counter - 1)
            <!NON_TAIL_RECURSIVE_CALL!>withWhen2<!>(0) == 0 -> withWhen2(counter - 1)
            else -> 1
        }

fun box() : String = if (withWhen2(100000) == 1) "OK" else "FAIL"
