class Foo(val bar: Bar)

class Bar {
    operator fun invoke() {}
}

fun test(foo: Foo?) {
    <caret>if (foo != null) {
        foo.bar()
    }
}
