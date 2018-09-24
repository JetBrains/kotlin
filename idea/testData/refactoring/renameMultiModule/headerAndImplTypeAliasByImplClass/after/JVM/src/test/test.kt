package test

class ZZZ

actual typealias Baz = ZZZ
actual class Bar

fun test() {
    val baz: Baz = Baz()
    val bar: Bar = Bar()
}