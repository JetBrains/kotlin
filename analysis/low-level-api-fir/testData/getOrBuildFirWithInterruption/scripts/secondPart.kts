// INTERRUPT_AT: 2

fun foo(a: Int): broken.lib.Foo = null!!

foo(1)
<expr>foo(2)</expr>
<expr_1>foo(3)</expr_1>
foo(4)

fun bar(a: Int): broken.lib.Foo = null!!

<expr_2>bar(1).result</expr_2>
<expr_3>bar(2)</expr_3>
<expr_4>bar(3)</expr_4>
bar(4)