// "Wrap with '?.let { ... }' call" "true"
// WITH_RUNTIME

interface Foo {
    val bar: ((Int) -> Unit)?
}

fun test(foo: Foo) {
    foo.bar<caret>(1)
}
