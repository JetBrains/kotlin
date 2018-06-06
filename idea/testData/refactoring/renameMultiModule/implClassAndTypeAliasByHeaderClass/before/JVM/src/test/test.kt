package test

actual class Foo
actual class Bar

fun test() {
    val foo: Foo = Foo()
    val bar: Bar = Bar()
}