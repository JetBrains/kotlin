class Foo<T>

fun <T> test(foo: Foo<T>, block: context(Foo<T>) () -> Unit) {
    block(foo)
}