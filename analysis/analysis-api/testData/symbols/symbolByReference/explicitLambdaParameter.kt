// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE
inline fun <T, R> T.use(block: (T) -> R): R {
    return block(this)
}

fun foo() {
    42.use { it ->
        i<caret>t.toString()
    }
}
