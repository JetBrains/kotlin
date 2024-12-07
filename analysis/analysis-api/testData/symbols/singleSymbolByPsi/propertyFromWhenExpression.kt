// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE

fun usage(i: Int) {
    when (val fo<caret>o = i + 1) {
        0 -> {}
        else -> {}
    }
}
