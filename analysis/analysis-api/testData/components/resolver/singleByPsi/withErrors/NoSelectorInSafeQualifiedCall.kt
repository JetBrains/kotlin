package testing

interface Foo

val otherFoo: Foo = makeFoo()

fun makeFoo(): Foo = Foo()

fun test() {
    <caret>makeFoo()?.
}