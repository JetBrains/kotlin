// TODO: muted automatically, investigate should it be ran for JS or not
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
