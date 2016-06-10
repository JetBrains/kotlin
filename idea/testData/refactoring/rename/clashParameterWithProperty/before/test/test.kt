package test

class Foo {
    var foo: Int = 0
    var bar: Int = 0
}

fun makeFoo(/*rename*/_foo: Int, _bar: Int) = Foo().apply {
    foo = _foo
    bar = _bar
}