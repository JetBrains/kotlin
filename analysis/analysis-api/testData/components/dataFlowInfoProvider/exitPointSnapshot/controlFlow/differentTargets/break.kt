fun test() {
    outer@ while (cond()) {
        consume(1)
        while (cond()) {
            consume(2)
            <expr>if (cond()) {
                break
            } else if (cond()) {
                break@outer
            }</expr>
        }
    }
}

fun consume(obj: Any?) {}

fun cond(): Boolean = true