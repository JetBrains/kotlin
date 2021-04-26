open class A {
    protected fun foo() {}
}

class B: A()

class C: A() {
    fun bar() {
        A().foo()
        B().foo()
    }
}

class D {
    fun qux() { B().<!INVISIBLE_REFERENCE!>foo<!>() }
}
