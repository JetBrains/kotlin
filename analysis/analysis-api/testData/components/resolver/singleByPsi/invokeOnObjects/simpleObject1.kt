object Foo {
    operator fun invoke() {}
}

fun test() {
    <expr>Foo</expr>()
}