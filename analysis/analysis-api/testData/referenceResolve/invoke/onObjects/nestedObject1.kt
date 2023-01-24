object Foo {
    object Helper {
        operator fun invoke() {}
    }
}

fun test() {
    Fo<caret>o.Helper()
}
