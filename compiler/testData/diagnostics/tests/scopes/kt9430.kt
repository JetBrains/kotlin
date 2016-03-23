open class A {
    protected fun foo() {}
}

class B: A()

class C: A() {
    fun bar() {
        A().<!INVISIBLE_MEMBER!>foo<!>()
        B().<!INVISIBLE_MEMBER!>foo<!>()
    }
}

class D {
    fun qux() { B().<!INVISIBLE_MEMBER!>foo<!>() }
}