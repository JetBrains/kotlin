package test

class ZZZ

actual typealias /*rename*/Foo = ZZZ
actual class Bar

fun test() {
    val foo: Foo = Foo()
    val bar: Bar = Bar()
}