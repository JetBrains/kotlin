package test

object Foo {
    operator fun invoke() {}
}

typealias FooAlias = Foo

fun test() {
    <caret>FooAlias()
}