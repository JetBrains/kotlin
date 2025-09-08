// INTERRUPT_AT: 1

fun consume(a: Any): Any = a

fun test(foo: broken.lib.Foo) {
    <expr>consume(1)</expr>
    <expr_1>if (a > 2) {
        consume(foo.result)
    }</expr_1>
    <expr_2>if (a > 3) {
        consume(3)
    }</expr_2>
    consume(4)
}

consume("A")
consume("B")
consume("C")