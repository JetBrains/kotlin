class A {
    fun f() {}
}

class B : A {
    fun g() {
        <!NOT_A_SUPERTYPE!>super<String><!>.<!UNRESOLVED_REFERENCE{LT}!><!UNRESOLVED_REFERENCE{PSI}!>f<!>()<!>
        super<A>.f()
    }
}
