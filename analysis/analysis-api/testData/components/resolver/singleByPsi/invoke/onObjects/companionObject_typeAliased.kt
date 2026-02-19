package test

class Foo private constructor() {
    companion object {
        operator fun invoke() {}
    }
}

typealias FooAlias = Foo

fun test() {
    <caret>FooAlias()
}