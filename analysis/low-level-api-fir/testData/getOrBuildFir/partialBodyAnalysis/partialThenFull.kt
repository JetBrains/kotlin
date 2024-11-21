fun foo() {
    consume(1)
    consume(2)
    <expr>consume(3)</expr>
    consume(4)
    <expr_1>consume(5)</expr_1>
}

fun consume(n: Int) {}