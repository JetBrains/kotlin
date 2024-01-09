class Foo

operator fun Foo.invoke() {}

fun foo() {
    val x = Foo()

    x()
}
