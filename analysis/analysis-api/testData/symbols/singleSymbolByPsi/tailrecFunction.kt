// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE_K1

tailrec fun fo<caret>o(i: Int): Int {
    if (i > 10) return i

    return foo(i + 1)
}