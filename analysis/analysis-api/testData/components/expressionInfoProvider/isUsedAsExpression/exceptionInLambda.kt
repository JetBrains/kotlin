fun test(text: String) {
    call {
        if (text.isNotEmpty()) {
            return@call text
        }
        throw <expr>IllegalArgumentException("Text is empty!")</expr>
    }
}

fun call(block: () -> String) = block()