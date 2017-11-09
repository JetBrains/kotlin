package test

expect class Foo
expect class Bar

fun test() {
    val foo: Foo = Foo()
    val bar: Bar = Bar()
}