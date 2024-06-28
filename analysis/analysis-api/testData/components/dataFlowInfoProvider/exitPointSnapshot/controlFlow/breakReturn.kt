fun test() {
    while (cond()) {
        <expr>if (foo() == 5) {
            break
        } else if (foo() == 6) {
            return
        }</expr>
        consume("foo")
    }
}

fun cond(): Boolean = true

fun foo(): Int = 0

fun consume(text: String?) = {}