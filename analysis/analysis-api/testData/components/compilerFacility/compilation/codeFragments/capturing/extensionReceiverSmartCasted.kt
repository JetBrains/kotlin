interface Foo

class FooImpl : Foo {
    val n: Int = 5
}

fun makeFoo(): Foo = FooImpl()

fun main() {
    makeFoo().apply {
        <caret>Unit
    }
}