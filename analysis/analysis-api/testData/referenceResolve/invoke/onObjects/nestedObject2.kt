object Foo {
    object Helper {
        operator fun invoke() {}
    }
}

fun test() {
    Foo.He<caret>lper()
}
