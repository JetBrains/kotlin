package a

fun <caret>test {
    val aFoo: Foo = Foo()
    val bFoo: b.Foo = b.Foo()
    val cFoo: c.Foo = c.Foo()
    val aBar: Foo.Bar = Foo.Bar()
    val bBar: b.Foo.Bar = b.Foo.Bar()
    val cBar: c.Foo.Bar = c.Foo.Bar()
}

class Test {
    fun foo() {
        test()
    }
}
