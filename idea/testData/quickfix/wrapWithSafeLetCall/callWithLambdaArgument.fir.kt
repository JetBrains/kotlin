// "Wrap with '?.let { ... }' call" "true"
// WITH_RUNTIME

interface Foo {
    val f: ((() -> Unit) -> String)?
}

fun test(foo: Foo) {
    bar(foo.<caret>f {})
}

fun bar(s: String) {}
