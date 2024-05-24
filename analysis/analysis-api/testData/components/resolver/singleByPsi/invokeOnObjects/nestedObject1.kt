object Foo {
    object Helper {
        operator fun invoke() {}
    }
}

fun test() {
    <expr>Foo</expr>.Helper()
}
