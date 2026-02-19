fun test() {
    var x = 0
    if (x > 0) {
        <expr>x = 1</expr>
    }
    consume(x)
}

fun consume(n: Int) {}