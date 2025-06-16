fun test(text: String) {
    call {
        5 + <expr>8</expr>
    }
}

fun call(block: () -> Int) = block()