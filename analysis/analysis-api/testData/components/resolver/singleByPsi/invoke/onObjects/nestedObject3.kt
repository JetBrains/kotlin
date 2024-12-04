package foo.bar.baz

object Foo {
    object Helper {
        operator fun invoke() {}
    }
}

fun test() {
    foo.bar.baz.Fo<caret>o.Helper()
}
