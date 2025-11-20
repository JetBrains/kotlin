class Foo<T>

fun <T> test(foo: Foo<T>, block: <expr>context(Foo<T>) () -> Unit</expr>) {
    block(foo)
}