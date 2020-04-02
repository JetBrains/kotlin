class Foo {
    operator fun plus(other: Foo): Foo = this
}

fun test_1() {
    val f1 = Foo()
    val f2 = Foo()
    val f3 = f1 + f2
}

fun test_2() {
    var f = Foo()
    f += Foo()
}