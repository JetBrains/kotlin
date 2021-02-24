class A {
    fun f() {}
}

class B : A {
    fun g() {
        <!NOT_A_SUPERTYPE!>super<String><!>.<!UNRESOLVED_REFERENCE!>f<!>()
        super<A>.f()
    }
}
