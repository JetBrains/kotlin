// INTERRUPT_AT: 2

fun foo(a: Int): broken.lib.Foo = null!!

<expr>foo(1)</expr>
<expr_1>foo(2)</expr_1>
<expr_2>foo(3).result</expr_2>
foo(4)

fun bar(a: Int): broken.lib.Foo = null!!

bar(1)
bar(2)
bar(3)
bar(4)