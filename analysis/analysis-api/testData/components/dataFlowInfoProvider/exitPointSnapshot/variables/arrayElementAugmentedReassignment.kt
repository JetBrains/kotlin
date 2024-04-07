fun test() {
    var x = intArrayOf(0)
    <expr>x[0] += 1</expr>
    consume(x)
}

fun consume(n: Int) {}