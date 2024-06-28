fun test() {
    <expr>val x = 0
    consume(1)</expr>
    consume(x)
}

fun consume(n: Int) {}