fun test() {
    x@ while (cond()) {
        <expr>consume(5)
        break@x</expr>
    }
}

fun cond(): Boolean = true

fun consume(n: Int) {}