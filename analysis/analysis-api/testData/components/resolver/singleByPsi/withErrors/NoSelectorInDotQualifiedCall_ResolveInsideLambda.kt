package testing

interface Foo

val otherFoo: Foo = makeFoo()

fun makeFoo(action: () -> Unit): Foo = Foo()

fun test() {}

fun test() {
    makeFoo {
        <caret>test()
    }.
}