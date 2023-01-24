package foo

object Foo {
    operator fun invoke() {}
}

fun test() {
    foo.<expr>Foo</expr>()
}