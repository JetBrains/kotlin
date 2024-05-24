object Foo {
    object Helper {
        operator fun invoke() {}
    }
}

fun test() {
    Foo.<expr>Helper</expr>()
}
