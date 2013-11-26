// !DIAGNOSTICS: -UNUSED_PARAMETER

tailRecursive fun test(x : Int, z : Any) : Int {
    if (x == 10) {
        return 1 + <!NON_TAIL_RECURSIVE_CALL!>test<!>(x - 1, "no tail")
    }
    if (x > 0) {
        return test(x - 1, "tail")
    }
    return 0
}

fun box() : String = if (test(1000000, "test") == 1) "OK" else "FAIL"