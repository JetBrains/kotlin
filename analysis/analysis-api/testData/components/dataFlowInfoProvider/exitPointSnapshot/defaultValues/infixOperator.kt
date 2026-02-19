fun test(bar: String) {
    consume("foo" <expr>join</expr> bar)
}

infix fun String.join(other: String): String = this + other

fun consume(text: String) {}