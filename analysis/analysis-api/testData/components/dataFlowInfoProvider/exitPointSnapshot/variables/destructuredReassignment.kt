// WITH_STDLIB

fun test() {
    var (x, y) = Pair(1, 2)
    <expr>x = 1</expr>
    consume(x)
}

fun consume(n: Int) {}