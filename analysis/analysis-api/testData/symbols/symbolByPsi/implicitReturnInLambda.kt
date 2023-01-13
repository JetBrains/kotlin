// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE
// DO_NOT_CHECK_SYMBOL_RESTORE_K1
fun foo() {
    val lam1 = { a: Int ->
        val b = 1
        a + b
    }

    val lam2 = { a: Int ->
        val c = 1
        if (a > 0)
            a + c
        else
            a - c
    }

    bar {
        if (it > 5) return
        val b = 1
        it + b
    }
}

private inline fun bar(lmbd: (Int) -> Int) {
    lmbd(1)
}
