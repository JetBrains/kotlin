package b

class Test {
    val aFoo: a.Foo = a.Foo()
    val bFoo: Foo = Foo()
    val cFoo: c.Foo = c.Foo()
    val aBar: a.Foo.Bar = a.Foo.Bar()
    val bBar: Foo.Bar = Foo.Bar()
    val cBar: c.Foo.Bar = c.Foo.Bar()
}

fun test() {
    a.foo()
    foo()
    c.foo()
}

fun Test.test() {
    a.foo()
    foo()
    c.foo()
}

var TEST: String
    get() = a.FOO + FOO + c.FOO
    set(value: String) {
        a.FOO = value
        FOO = value
        c.FOO = value
    }

var Test.TEST: String
    get() = a.FOO + FOO + c.FOO
    set(value: String) {
        a.FOO = value
        FOO = value
        c.FOO = value
    }

enum class My(val x: Int) {
    FIRST(4)
}
