// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

tailrec fun test(x : Int) : Int {
    if (x == 1) {
        if (x != 1) {
            <!NON_TAIL_RECURSIVE_CALL!>test<!>(0)
            return test(0)
        } else {
            return test(x + <!NON_TAIL_RECURSIVE_CALL!>test<!>(0))
        }
    } else if (x > 0) {
        return test(x - 1)
    }
    return -1
}

fun box() : String = if (test(1000000) == -1) "OK" else "FAIL"
