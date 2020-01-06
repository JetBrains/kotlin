// HIGHLIGHT: INFORMATION
fun test(foo: Foo?) {
    <caret>if (foo != null) foo()
}

class Foo {
    operator fun invoke() {}
}