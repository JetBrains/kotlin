fun test() {
    with(Foo()) {
        call()
    }
}

context(Foo)
fun call() {
    <caret>val x = 0
}

class Foo {
    val foo: String = "foo"
}

// LANGUAGE: +ContextReceivers