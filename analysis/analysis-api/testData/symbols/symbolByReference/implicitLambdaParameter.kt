// DO_NOT_REQUIRE_SYMBOL_RESTORATION_K1

inline fun <T, R> T.use(block: (T) -> R): R {
    return block(this)
}

fun foo() {
    42.use { i<caret>t.toString() }
}
