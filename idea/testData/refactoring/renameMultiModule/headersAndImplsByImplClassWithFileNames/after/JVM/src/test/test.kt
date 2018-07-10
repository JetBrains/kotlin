package test

actual class Baz
actual class Bar

fun test() {
    val baz: Baz = Baz()
    val bar: Bar = Bar()
}