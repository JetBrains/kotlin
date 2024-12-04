class Foo {
    companion object {
        operator fun invoke(i: Int): String {}
    }
}

fun take() {
    <expr>Foo</expr>(10)
}