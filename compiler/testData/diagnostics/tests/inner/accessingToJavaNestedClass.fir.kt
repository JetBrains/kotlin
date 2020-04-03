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

    val bc: B.NC = B.<!UNRESOLVED_REFERENCE!>NC<!>()
    val bic: B.IC = B().IC()
    val bi: B.NI? = null

    val cc: C.NC = C.<!UNRESOLVED_REFERENCE!>NC<!>()
    val ci: C.NI? = null

    val dc: D.NC = D.<!UNRESOLVED_REFERENCE!>NC<!>()
    val dic: D.IC = D().IC()
    val di: D.NI? = null

    val kc: K.NC = K.<!UNRESOLVED_REFERENCE!>NC<!>()
    val kic: K.IC = K().IC()
    val ki: K.NI? = null
}
