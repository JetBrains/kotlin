interface Foo1 : Foo2 {
    override fun f<caret>oo()
}

interface Foo2 : Foo3 {
    override fun foo()
}

interface Foo3 : Foo1 {
    override fun foo()
}
