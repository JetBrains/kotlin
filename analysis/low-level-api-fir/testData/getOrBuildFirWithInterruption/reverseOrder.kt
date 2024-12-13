// INTERRUPT_AT: 0

fun test(foo: broken.lib.Foo) {
    consume(1)
    <expr_2>consume(2)</expr_2>
    <expr_1>consume(3)</expr_1>
    <expr>consume(foo.result)</expr>
    consume(5)
}

fun consume(n: Int) {}