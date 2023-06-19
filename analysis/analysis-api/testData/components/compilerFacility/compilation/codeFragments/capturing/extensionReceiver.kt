fun test() {
    with("Hello, world!") {
        with(Foo()) {
            <caret>val x = 0
        }
    }
}

class Foo {
    val foo: String = "foo"
}