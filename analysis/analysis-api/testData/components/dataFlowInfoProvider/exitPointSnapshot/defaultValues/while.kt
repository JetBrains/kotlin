fun test() {
    var x = 0
    <expr>consume(x)
    while (cond()) {
        consume(++x)
    }</expr>
}

fun cond(): Boolean = true

fun consume(n: Int) {}