
<!NO_TAIL_CALLS_FOUND!>tailrec<!> fun test(counter : Int) : Int {
    if (counter == 0) return 0

    try {
        // do nothing
    } finally {
        return <!TAIL_RECURSION_IN_TRY_IS_NOT_SUPPORTED!>test<!>(counter - 1)
    }
}

fun box() : String = if (test(3) == 0) "OK" else "FAIL"
