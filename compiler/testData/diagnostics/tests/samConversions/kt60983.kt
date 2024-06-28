// MODULE: m1
// FILE: J.java
public class J {
    public static class C {}
    public class C2 {}
    public interface I {
        void x();
    }
}

// FILE: test.kt
class K {
    open class C
    inner class C2
    fun interface I {
        fun x()
    }
}

fun J.testJ() {
    <!UNRESOLVED_REFERENCE!>C<!>()
    C2()
    <!UNRESOLVED_REFERENCE!>I<!> {}
}

fun testJ2(j: J) {
    j.<!UNRESOLVED_REFERENCE!>C<!>()
    j.C2()
    j.<!RESOLUTION_TO_CLASSIFIER!>I<!> {}
}

fun K.testK() {
    <!UNRESOLVED_REFERENCE!>C<!>()
    C2()
    <!UNRESOLVED_REFERENCE!>I<!> {}
}

fun testK2(k: K) {
    k.<!UNRESOLVED_REFERENCE!>C<!>()
    k.C2()
    k.<!RESOLUTION_TO_CLASSIFIER!>I<!> {}
}

// MODULE: m2(m1)
// FILE: testResolutionContinues.kt
fun J.testResolutionContinues() {
    acceptI(I {})
}

fun K.testResolutionContinues() {
    acceptI(I {})
}

fun interface I {
    fun x()
}

fun acceptI(i: I) {}