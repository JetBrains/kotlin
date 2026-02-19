interface Fo<caret>o1 : Foo2 {
    override fun foo()
}

interface Foo2 : Foo3 {
    override fun foo()
}

interface Foo3 : Foo1 {
    override fun foo()
}
