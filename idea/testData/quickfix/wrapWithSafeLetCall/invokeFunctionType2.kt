// "Wrap with '?.let { ... }' call" "true"
// WITH_RUNTIME

interface Foo {
    val bar: ((Int) -> Unit)?
}

fun Foo.test() {
    <caret>bar(1)
}