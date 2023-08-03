package test

class Foo

fun test() {
    val foo = Foo()
    <caret>consume(foo)
}

fun consume(foo: Foo) {}