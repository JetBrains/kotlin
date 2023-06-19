fun test() {
    call(Foo())
}

context(Foo)
fun call() {
    <caret>val x = 0
}

class Foo {
    val foo: String = "foo"
}