class Bar {
    fun invoke(x: Int, y: Int) {}
}

class Foo {
    val set: Bar = Bar()
}

fun Foo.get(x: Int): Int = x

fun test(foo: Foo) {
    foo[1] += 2
}