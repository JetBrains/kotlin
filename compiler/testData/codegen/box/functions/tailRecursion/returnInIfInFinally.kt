// !DIAGNOSTICS: -UNUSED_PARAMETER

<!NO_TAIL_CALLS_FOUND!>tailRecursive fun test(counter : Int, a : Any) : Int<!> {
    if (counter == 0) return 0

    try {
        // do nothing
    } finally {
        if (counter > 0) {
            return <!TAIL_RECURSION_IN_TRY_IS_NOT_SUPPORTED!>test<!>(counter - 1, "no tail")
        }
    }

    return -1
}

fun box() : String = if (test(3, "test") == 0) "OK" else "FAIL"