class Foo

operator fun Foo.invoke() {}

fun foo() {
    val <!UNUSED_VARIABLE!>x<!> = Foo()

    x()
}
