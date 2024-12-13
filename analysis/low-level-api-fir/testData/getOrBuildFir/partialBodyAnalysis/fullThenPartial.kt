fun foo() {
    consume(1)
    consume(2)
    <expr_1>consume(3)</expr_1>
    consume(4)
    <expr>consume(5)</expr>
}

fun consume(n: Int) {}