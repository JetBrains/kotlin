// FIR_IDENTICAL
// SKIP_TXT
// FILE: A.java
public class A {
    public String get1() {
        return "";
    }
}

// FILE: main.kt
fun foo(a: A) {
    a.<!UNRESOLVED_REFERENCE!>`1`<!>
    a.get1()
}
