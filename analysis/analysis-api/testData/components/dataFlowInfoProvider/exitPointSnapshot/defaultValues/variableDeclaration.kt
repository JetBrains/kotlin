fun test() {
    <expr>consume(1)
    val x = 2</expr>
    consume(x)
}

fun consume(n: Int) {}