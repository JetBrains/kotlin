open class A {
    open protected fun foo() { }
    open protected fun foobaz() { }

    fun bar(x: B) {
        x.foo() // OK, foo declared in A
        x.<!HIDDEN!>baz<!>() // Declared in B
        x.<!HIDDEN!>foobaz<!>() // Declared in B
    }
}

class B : A() {
    protected fun baz() {}
    override fun foobaz() {}
}
