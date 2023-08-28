// FIR_IDENTICAL
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

    val bc: B.<!UNRESOLVED_REFERENCE!>NC<!> = B.<!UNRESOLVED_REFERENCE!>NC<!>()
    val bic: B.<!UNRESOLVED_REFERENCE!>IC<!> = B().IC()
    val bi: B.<!UNRESOLVED_REFERENCE!>NI<!>? = null

    val cc: C.<!UNRESOLVED_REFERENCE!>NC<!> = C.<!UNRESOLVED_REFERENCE!>NC<!>()
    val ci: C.<!UNRESOLVED_REFERENCE!>NI<!>? = null

    val dc: D.<!UNRESOLVED_REFERENCE!>NC<!> = D.<!UNRESOLVED_REFERENCE!>NC<!>()
    val dic: D.<!UNRESOLVED_REFERENCE!>IC<!> = D().IC()
    val di: D.<!UNRESOLVED_REFERENCE!>NI<!>? = null
}
