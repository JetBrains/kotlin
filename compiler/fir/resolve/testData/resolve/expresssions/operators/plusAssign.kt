operator fun Foo.plusAssign(x: Any) {}

class Foo {
    operator fun plusAssign(x: Foo) {}
    operator fun plusAssign(x: String) {}
}

fun test_1() {
    val f = Foo()
    f <!UNRESOLVED_REFERENCE!>+<!> f
}

fun test_2() {
    val f = Foo()
    f += f
}

fun test_3(f: Foo) {
    f += f
    f += ""
    f += 1
}