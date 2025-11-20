class Foo
interface Bar

fun test(foo: Foo, bar: Bar, block: <expr>context(Foo, Bar) () -> Unit</expr>) {
    block(foo, bar)
}