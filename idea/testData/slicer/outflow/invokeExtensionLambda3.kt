// FLOW: OUT

fun String.foo(<caret>p: String) {
    val v = bar(p) { this }
}

fun <T, R> bar(receiver: T, block: T.() -> R): R {
    val b = block
    return receiver.b()
}
