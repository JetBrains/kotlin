package foo

object Foo {
    operator fun invoke() {}
}

fun test() {
    foo.Fo<caret>o()
}