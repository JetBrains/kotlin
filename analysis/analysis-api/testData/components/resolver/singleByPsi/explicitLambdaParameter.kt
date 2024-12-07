inline fun <T, R> T.use(block: (T) -> R): R {
    return block(this)
}

fun foo() {
    42.use { it ->
        i<caret>t.toString()
    }
}
