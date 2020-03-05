// FILE: A.java
public interface A {
    class A_S {

    }
}

// FILE: B.java
public class B {
    static class B_S {

    }
    class B_ {

    }
}

// FILE: C.java
public class C extends B implements A {

}

// FILE: 1.kt
interface E {
    class E_S
}

open class D: C(), E

// FILE: F.java
public class F extends D {

}

// FILE: 2.kt
class X: D() {
    init {
        B_()
        B.<!UNRESOLVED_REFERENCE!>B_<!>()
        C.<!UNRESOLVED_REFERENCE!>B_<!>()
        D.<!UNRESOLVED_REFERENCE!>B_<!>()
        X.<!UNRESOLVED_REFERENCE!>B_<!>()

        A_S()
        A.A_S()
        C.<!UNRESOLVED_REFERENCE!>A_S<!>()
        D.<!UNRESOLVED_REFERENCE!>A_S<!>()
        X.<!UNRESOLVED_REFERENCE!>A_S<!>()

        B_S()
        B.B_S()
        C.<!UNRESOLVED_REFERENCE!>B_S<!>()
        D.<!UNRESOLVED_REFERENCE!>B_S<!>()
        X.<!UNRESOLVED_REFERENCE!>B_S<!>()

        E_S()
        E.E_S()
        D.<!UNRESOLVED_REFERENCE!>E_S<!>()
        X.<!UNRESOLVED_REFERENCE!>E_S<!>()
    }
}

class Y: F() {
    init {

        B_()
        F.<!UNRESOLVED_REFERENCE!>B_<!>()
        Y.<!UNRESOLVED_REFERENCE!>B_<!>()

        A_S()
        F.<!UNRESOLVED_REFERENCE!>A_S<!>()
        Y.<!UNRESOLVED_REFERENCE!>A_S<!>()

        B_S()
        F.<!UNRESOLVED_REFERENCE!>B_S<!>()
        Y.<!UNRESOLVED_REFERENCE!>B_S<!>()

        E_S()
        F.<!UNRESOLVED_REFERENCE!>E_S<!>()
        Y.<!UNRESOLVED_REFERENCE!>E_S<!>()
    }
}
