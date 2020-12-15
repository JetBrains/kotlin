/*
 * We see same constructor of `B` in two scopes
 */

open class A() {
    class B() : A() {
        fun copy() = B()
    }

    open class C() {
        fun copy() = C()
    }
}

class D : A.C() {
    fun foo() {
        val a = A()
        val ac = A.C()

        val c = <!UNRESOLVED_REFERENCE{LT}!><!UNRESOLVED_REFERENCE{PSI}!>C<!>()<!> // shouldn't resolve
    }
}

class E : A() {
    fun foo() {
        val a = A()
        val c = C()
    }
}
