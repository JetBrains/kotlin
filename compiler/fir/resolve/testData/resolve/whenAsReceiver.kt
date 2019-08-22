fun <T, R> T.also(block: () -> R): R {
    null!!
}

fun foo(b: Boolean) {
    val x = when (b) {
        true -> a
        else -> null
    }?.also {
        1
    }
}