// INTERRUPT_AT: 0

fun test(foo: broken.lib.Foo) {
    consume(1)
    <expr>consume(foo.result)</expr>
    <expr_1>consume(3)</expr_1>
    <expr_2>consume(4)</expr_2>
    <expr_3>consume(5)</expr_3>
}

fun consume(obj: Any) {}