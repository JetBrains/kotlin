// FILE: A.java
public interface A {
    public class A_S { // static

    }
}

// FILE: B.java
public class B {
    public static class B_S {

    }
    public class B_ {

    }
}

// FILE: C.java
public class C extends B implements A {

}

// FILE: 1.kt

class X: A {
    val a_s: A_S = null!!

    init {
        A_S()
        A.A_S()
        X.<!UNRESOLVED_REFERENCE!>A_S<!>()
    }

    object xD {
        val a_: A_S = null!!

        init {
            A_S()
        }
    }
}

class Y: B() {
    val b_: B_ = null!!
    val b_s: B_S = null!!

    init {
        B_()
        B.<!UNRESOLVED_REFERENCE!>B_<!>()
        Y.<!UNRESOLVED_REFERENCE!>B_<!>()

        B_S()
        B.B_S()
        Y.<!UNRESOLVED_REFERENCE!>B_S<!>()
    }

    object X {
        val b_: B_ = null!!
        val b_s: B_S = null!!

        init {
            B_()
            B_S()
        }
    }
}

class Z: C() {
    val a_s: A_S = null!!
    val b_: B_ = null!!
    val b_s: B_S = null!!

    init {
        A_S()
        B_()
        B_S()
    }

    object X {
        val a_s: A_S = null!!
        val b_: B_ = null!!
        val b_s: B_S = null!!

        init {
            A_S()
            B_()
            B_S()
        }
    }
}
