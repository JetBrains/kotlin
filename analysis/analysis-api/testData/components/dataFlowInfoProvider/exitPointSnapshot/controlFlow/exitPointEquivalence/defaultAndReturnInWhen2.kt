fun example(i: Int) {
    when (i) {
        1 -> {
            <expr>if (i > 5) {
                consume("true")
            }
            else {
                consume("false")
                return
            }
            consume("!!")</expr>
        }
        2 -> {
            consume("another")
        }
    }
}

fun consume(obj: Any?) {}