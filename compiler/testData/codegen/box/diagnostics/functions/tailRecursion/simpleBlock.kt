// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

tailrec fun test(x : Int) : Int =
    if (x == 1) {
        <!NON_TAIL_RECURSIVE_CALL!>test<!>(x - 1)
        1 + <!NON_TAIL_RECURSIVE_CALL!>test<!>(x - 1)
    } else if (x > 0) {
        test(x - 1)
    } else {
        0
    }

fun box() : String = if (test(1000000) == 1) "OK" else "FAIL"
