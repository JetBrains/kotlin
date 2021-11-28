open class A {
    protected fun foo() {}
}

class B: A()

class C: A() {
    fun bar() {
        A().<!INVISIBLE_REFERENCE!>foo<!>()
        B().<!INVISIBLE_REFERENCE!>foo<!>()
    }
}

class D {
    fun qux() { B().<!INVISIBLE_REFERENCE!>foo<!>() }
}
