package test

expect class Baz
expect class Bar

fun test() {
    val baz: Baz = Baz()
    val bar: Bar = Bar()
}