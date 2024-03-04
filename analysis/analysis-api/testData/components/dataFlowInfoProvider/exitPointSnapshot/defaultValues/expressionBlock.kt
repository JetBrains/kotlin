fun test(flag: Boolean) {
    if (flag) <expr>{
        consume(1)
    }</expr> else {
        consume(2)
    }
}

fun consume(n: Int) {}