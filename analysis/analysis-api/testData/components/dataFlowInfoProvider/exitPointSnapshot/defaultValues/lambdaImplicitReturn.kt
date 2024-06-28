fun test(flag: Boolean) {
    block {
        consume("before")

        <expr>if (flag) {
            return@block 1
        }

        2</expr>
    }
}

fun block(block: () -> Int) {
    block()
}

fun consume(text: String) {}