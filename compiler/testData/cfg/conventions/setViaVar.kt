class Bar {
    fun invoke(x: Int, y: Int) {}
}

class Foo {
    val set: Bar = Bar()
}

fun test1(foo: Foo) {
    foo[1] = 2
}

fun test2(foo: Foo, set: Foo.(Int, Int) -> Int) {
    foo[1] = 2
}