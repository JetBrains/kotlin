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

    val bc: <!OTHER_ERROR!>B.NC<!> = B.<!UNRESOLVED_REFERENCE!>NC<!>()
    val bic: <!OTHER_ERROR!>B.IC<!> = B().IC()
    val bi: <!OTHER_ERROR!>B.NI?<!> = null

    val cc: <!OTHER_ERROR!>C.NC<!> = C.<!UNRESOLVED_REFERENCE!>NC<!>()
    val ci: <!OTHER_ERROR!>C.NI?<!> = null

    val dc: <!OTHER_ERROR!>D.NC<!> = D.<!UNRESOLVED_REFERENCE!>NC<!>()
    val dic: <!OTHER_ERROR!>D.IC<!> = D().IC()
    val di: <!OTHER_ERROR!>D.NI?<!> = null

    val kc: <!OTHER_ERROR!>K.NC<!> = K.<!UNRESOLVED_REFERENCE!>NC<!>()
    val kic: <!OTHER_ERROR!>K.IC<!> = K().IC()
    val ki: <!OTHER_ERROR!>K.NI?<!> = null
}
