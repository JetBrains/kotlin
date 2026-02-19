fun test(): Int {
    while (cond()) {
        <expr>if (foo() == 5) {
            return 1
        } else if (foo() == 6) {
            continue
        }</expr>
        consume("foo")
    }

    return 0
}

fun cond(): Boolean = true

fun foo(): Int = 0

fun consume(text: String?) = {}