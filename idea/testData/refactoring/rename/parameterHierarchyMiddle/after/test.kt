interface Foo {
    fun foo(s: String)
}

open class Foo1 : Foo {
    override fun foo(x: String) { }
}

class Foo2 : Foo {
    override fun foo(s: String) { }
}

class Foo3 : Foo1() {
    override fun foo(x: String) { }
}