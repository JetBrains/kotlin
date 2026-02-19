fun test() {
    var x = 0
    <expr>++x</expr>
    consume(x)
}

fun consume(n: Int) {}