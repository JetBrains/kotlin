// !DIAGNOSTICS: -UNUSED_PARAMETER

tailRecursive fun test(x : Int, a : Any) : Int {
    if (x == 1) {
        if (x != 1) {
            <!NON_TAIL_RECURSIVE_CALL!>test<!>(0, "no tail")
            return test(0, "tail")
        } else {
            return test(x + <!NON_TAIL_RECURSIVE_CALL!>test<!>(0, "no tail"), "tail")
        }
    } else if (x > 0) {
        return test(x - 1, "tail")
    }
    return -1
}

fun box() : String = if (test(1000000, "test") == -1) "OK" else "FAIL"