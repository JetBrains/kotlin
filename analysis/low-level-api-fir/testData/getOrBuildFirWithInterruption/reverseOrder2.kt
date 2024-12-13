// INTERRUPT_AT: 1

fun test(foo: broken.lib.Foo) {
    consume(1)
    <expr_2>consume(2)</expr_2>
    <expr>consume(3)</expr>
    <expr_1>consume(foo.result)</expr_1>
    consume(5)
}

fun consume(n: Int) {}