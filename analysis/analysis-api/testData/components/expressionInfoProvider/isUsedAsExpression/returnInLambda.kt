fun test(text: String) {
    call {
        if (text.isNotEmpty()) {
            <expr>return@call text</expr>
        }
        throw IllegalArgumentException("Text is empty!")
    }
}

fun call(block: () -> String) = block()