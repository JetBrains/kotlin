class Bar {
    fun invoke(x: Int): Int = x
}

class Foo {
    val get: Bar = Bar()
}

fun test1(foo: Foo) {
    foo[1]
}

fun test2(foo: Foo, get: Foo.(Int) -> Int) {
    foo[1]
}