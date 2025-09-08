// Each individual statement becomes a separate initializer
// INTERRUPT_AT: 1

fun foo(a: Int): broken.lib.Foo = null!!

<expr_2>foo(1)</expr_2>
<expr_1>foo(2).result</expr_1>
<expr>foo(3)</expr>
foo(4)

fun bar(a: Int): broken.lib.Foo = null!!

bar(1)
bar(2)
bar(3)
bar(4)