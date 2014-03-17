package b

object Test {
    val aFoo: a.Foo = a.Foo()
    val bFoo: Foo = Foo()
    val cFoo: c.Foo = c.Foo()
    val aBar: a.Foo.Bar = a.Foo.Bar()
    val bBar: Foo.Bar = Foo.Bar()
    val cBar: c.Foo.Bar = c.Foo.Bar()
}