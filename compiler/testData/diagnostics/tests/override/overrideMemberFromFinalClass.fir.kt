class Foo {
    open fun openFoo() {}
    fun finalFoo() {}
}

class Bar : Foo() {
    override fun openFoo() {}
    override fun finalFoo() {}
}


open class A1 {
    open fun foo() {}
}

class B1 : A1()
class C1 : B1() {
    override fun foo() {}
}

abstract class A2 {
    abstract fun foo()
}

class B2 : A2()
class C2 : B2() {
    override fun foo() {}
}