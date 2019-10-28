class Foo {
    operator fun plus(f: Foo): Foo {}
    operator fun plusAssign(f: Foo) {}
}

fun test() {
    var f = Foo()
    f += f
}