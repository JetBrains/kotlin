// !DIAGNOSTICS: -UNUSED_PARAMETER

tailRecursive fun test(x : Int, e : Any) : Unit {
    if (x == 1) {
        test(x - 1, "tail")
    } else if (x == 2) {
        test(x - 1, "tail")
        return
    } else if (x == 3) {
        <!NON_TAIL_RECURSIVE_CALL!>test<!>(x - 1, "no tail")
        if (x == 3) {
            test(x - 1, "tail")
        }
        return
    } else if (x > 0) {
        test(x - 1, "tail")
    }
}

fun box() : String {
    test(1000000, "test")
    return "OK"
}