class Bar {
    fun invoke(x: Int): Int = x
    fun invoke(x: Int, y: Int) {}
}

class Foo {
    val get: Bar = Bar()
    val set: Bar = Bar()
}

fun test(foo: Foo) {
    foo[1] += 2
}