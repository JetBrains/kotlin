// DO_NOT_CHECK_SYMBOL_RESTORE_K1
// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE

inline fun <T, R> T.use(block: (T) -> R): R {
    return block(this)
}

fun foo() {
    42.use { i<caret>t.toString() }
}
