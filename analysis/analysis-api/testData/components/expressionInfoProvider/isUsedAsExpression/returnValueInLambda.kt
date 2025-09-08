fun test(text: String) {
    call {
        if (text.isNotEmpty()) {
            return@call <expr>text</expr>
        }
        throw IllegalArgumentException("Text is empty!")
    }
}

fun call(block: () -> String) = block()