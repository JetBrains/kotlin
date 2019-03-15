package test

expect class /*rename*/Foo
expect class Bar

fun test() {
    val foo: Foo = Foo()
    val bar: Bar = Bar()
}