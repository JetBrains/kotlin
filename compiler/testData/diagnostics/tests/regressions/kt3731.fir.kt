// KT-3731 Resolve & inner class

class A {
    fun foo() {}
    fun bar(f: A.() -> Unit = {}) = <!INAPPLICABLE_CANDIDATE!>f<!>()
}

class B {
    class D {
        init {
            A().bar {
                this.<!UNRESOLVED_REFERENCE!>foo<!>()
                <!UNRESOLVED_REFERENCE!>foo<!>()
            }
        }
    }
}
