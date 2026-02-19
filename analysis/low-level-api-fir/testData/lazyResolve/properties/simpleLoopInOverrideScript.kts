interface Foo1 : Foo2 {
    override fun foo()
    override val st<caret>r: String
}

interface Foo2 : Foo3 {
    override fun foo()
    override val str: String
}

interface Foo3 : Foo1 {
    override fun foo()
    override val str: String
}
