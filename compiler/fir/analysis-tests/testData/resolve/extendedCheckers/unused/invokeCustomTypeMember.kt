class Foo {
    operator fun invoke() {}
}

fun foo() {
    val <!UNUSED_VARIABLE!>x<!> = Foo()

    x()
}
