// !DIAGNOSTICS: -UNUSED_VARIABLE

open class A {
    class NC {}
    inner class IC {}
    interface NI {}
}

interface I {
    class NC {}
    interface NI {}
}

class B : A() {

}

class C : I {

}

class D : A(), I {

}

fun test() {
    val ac: A.NC = A.NC()
    val aic: A.IC = A().IC()
    val ai: A.NI? = null

    val ic: I.NC = I.NC()
    val ii: I.NI? = null

    val bc: <!UNRESOLVED_REFERENCE!>B.NC<!> = B.<!UNRESOLVED_REFERENCE!>NC<!>()
    val bic: <!UNRESOLVED_REFERENCE!>B.IC<!> = B().IC()
    val bi: <!UNRESOLVED_REFERENCE!>B.NI<!>? = null

    val cc: <!UNRESOLVED_REFERENCE!>C.NC<!> = C.<!UNRESOLVED_REFERENCE!>NC<!>()
    val ci: <!UNRESOLVED_REFERENCE!>C.NI<!>? = null

    val dc: <!UNRESOLVED_REFERENCE!>D.NC<!> = D.<!UNRESOLVED_REFERENCE!>NC<!>()
    val dic: <!UNRESOLVED_REFERENCE!>D.IC<!> = D().IC()
    val di: <!UNRESOLVED_REFERENCE!>D.NI<!>? = null
}
