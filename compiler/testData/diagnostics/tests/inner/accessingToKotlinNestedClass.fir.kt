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

    val bc: <!OTHER_ERROR!>B.NC<!> = B.<!UNRESOLVED_REFERENCE!>NC<!>()
    val bic: <!OTHER_ERROR!>B.IC<!> = B().IC()
    val bi: <!OTHER_ERROR!>B.NI?<!> = null

    val cc: <!OTHER_ERROR!>C.NC<!> = C.<!UNRESOLVED_REFERENCE!>NC<!>()
    val ci: <!OTHER_ERROR!>C.NI?<!> = null

    val dc: <!OTHER_ERROR!>D.NC<!> = D.<!UNRESOLVED_REFERENCE!>NC<!>()
    val dic: <!OTHER_ERROR!>D.IC<!> = D().IC()
    val di: <!OTHER_ERROR!>D.NI?<!> = null
}
