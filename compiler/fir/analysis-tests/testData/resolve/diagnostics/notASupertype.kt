open class A {
    fun f() {}
}

class B : <!SUPERTYPE_NOT_INITIALIZED!>A<!> {
    fun g() {
        <!NOT_A_SUPERTYPE!>super<String><!>.<!UNRESOLVED_REFERENCE!>f<!>()
        super<A>.f()
    }
}
