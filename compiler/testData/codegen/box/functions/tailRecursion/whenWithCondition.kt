// !DIAGNOSTICS: -UNUSED_PARAMETER

tailRecursive fun withWhen(counter : Int, x : Any) : Int =
        when (counter) {
            0 -> counter
            50 -> 1 + <!NON_TAIL_RECURSIVE_CALL!>withWhen<!>(counter - 1, "no tail")
            else -> withWhen(counter - 1, "tail")
        }

fun box() : String = if (withWhen(100000, "test") == 1) "OK" else "FAIL"