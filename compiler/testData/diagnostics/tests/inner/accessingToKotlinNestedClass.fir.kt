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

    val bc: B.NC = B.<!UNRESOLVED_REFERENCE!>NC<!>()
    val bic: B.IC = B().IC()
    val bi: B.NI? = null

    val cc: C.NC = C.<!UNRESOLVED_REFERENCE!>NC<!>()
    val ci: C.NI? = null

    val dc: D.NC = D.<!UNRESOLVED_REFERENCE!>NC<!>()
    val dic: D.IC = D().IC()
    val di: D.NI? = null
}
