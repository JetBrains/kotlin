fun <T, R> T.also(block: () -> R): R {
    return <!UNRESOLVED_REFERENCE!>null!!<!>
}

fun foo(b: Boolean, a: Int) {
    val x = when (b) {
        true -> a
        else -> null
    }?.also {
        1
    }
}