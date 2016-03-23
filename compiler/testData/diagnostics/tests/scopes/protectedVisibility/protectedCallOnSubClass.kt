open class A {
    open protected fun foo() { }
    open protected fun foobaz() { }

    fun bar(x: B) {
        x.foo() // OK, foo declared in A
        x.<!INVISIBLE_MEMBER!>baz<!>() // Declared in B
        x.<!INVISIBLE_MEMBER!>foobaz<!>() // Declared in B
    }
}

class B : A() {
    protected fun baz() {}
    override fun foobaz() {}
}
