fun test(text: String) {
    call {
        val x = listOf(5)
        <expr>x.size</expr>
    }
}

fun call(block: () -> Int) = block()