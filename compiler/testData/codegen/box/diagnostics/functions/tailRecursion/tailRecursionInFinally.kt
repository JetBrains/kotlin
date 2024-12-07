
<!NO_TAIL_CALLS_FOUND!>tailrec<!> fun test(go: Boolean) : Unit {
    if (!go) return
    try {
        <!TAIL_RECURSION_IN_TRY_IS_NOT_SUPPORTED!>test<!>(false)
    } catch (any : Exception) {
        <!TAIL_RECURSION_IN_TRY_IS_NOT_SUPPORTED!>test<!>(false)
    } finally {
        <!TAIL_RECURSION_IN_TRY_IS_NOT_SUPPORTED!>test<!>(false)
    }
}

fun box(): String {
    test(true)
    return "OK"
}
