package a

object <caret>Test {
    val aFoo: Foo = Foo()
    val bFoo: b.Foo = b.Foo()
    val cFoo: c.Foo = c.Foo()
    val aBar: Foo.Bar = Foo.Bar()
    val bBar: b.Foo.Bar = b.Foo.Bar()
    val cBar: c.Foo.Bar = c.Foo.Bar()
}

fun test(): Test {
    return Test()
}
