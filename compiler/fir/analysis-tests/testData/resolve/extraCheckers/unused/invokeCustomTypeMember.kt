class Foo {
    operator fun invoke() {}
}

fun foo() {
    val x = Foo()

    x()
}
