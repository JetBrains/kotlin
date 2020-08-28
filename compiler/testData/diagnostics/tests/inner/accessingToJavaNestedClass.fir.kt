// !DIAGNOSTICS: -UNUSED_VARIABLE

// FILE: A.java

public class A {
    static class NC {}
    class IC {}
    static interface NI {}
}

// FILE: I.java

public interface I {
    class NC {}
    interface NI {}
}

// FILE: B.java

public class B extends A {

}

// FILE: C.java

public class C implements I {

}

// FILE: D.java

public class D extends A implements I {

}

// FILE: K.kt

class K : D()

// FILE: test.kt

fun test() {
    val ac: A.NC = A.NC()
    val aic: A.IC = A().IC()
    val ai: A.NI? = null

    val ic: I.NC = I.NC()
    val ii: I.NI? = null

    val bc: <!UNRESOLVED_REFERENCE!>B.NC<!> = B.<!UNRESOLVED_REFERENCE!>NC<!>()
    val bic: <!UNRESOLVED_REFERENCE!>B.IC<!> = B().IC()
    val bi: <!UNRESOLVED_REFERENCE!>B.NI?<!> = null

    val cc: <!UNRESOLVED_REFERENCE!>C.NC<!> = C.<!UNRESOLVED_REFERENCE!>NC<!>()
    val ci: <!UNRESOLVED_REFERENCE!>C.NI?<!> = null

    val dc: <!UNRESOLVED_REFERENCE!>D.NC<!> = D.<!UNRESOLVED_REFERENCE!>NC<!>()
    val dic: <!UNRESOLVED_REFERENCE!>D.IC<!> = D().IC()
    val di: <!UNRESOLVED_REFERENCE!>D.NI?<!> = null

    val kc: <!UNRESOLVED_REFERENCE!>K.NC<!> = K.<!UNRESOLVED_REFERENCE!>NC<!>()
    val kic: <!UNRESOLVED_REFERENCE!>K.IC<!> = K().IC()
    val ki: <!UNRESOLVED_REFERENCE!>K.NI?<!> = null
}
