fun test(text: String) {
    call {
        if (text.isNotEmpty()) {
            return@call text
        }
        <expr>throw IllegalArgumentException("Text is empty!")</expr>
    }
}

fun call(block: () -> String) = block()