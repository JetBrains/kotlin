// IGNORE_REVERSED_RESOLVE
open class A {
    protected fun foo() {}

    init {
        B.foo() // Ok, receiver (B.Companion) is subtype of A
        (B.Companion).foo()
    }
}

class B {
    companion object : A()
}

class C: A() {
    init {
        B.<!INVISIBLE_MEMBER!>foo<!>() // Error: receiver is not suitable
    }
}
