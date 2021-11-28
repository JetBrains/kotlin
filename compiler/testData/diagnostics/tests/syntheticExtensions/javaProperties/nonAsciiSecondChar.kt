// FIR_IDENTICAL
// SKIP_TXT
// FILE: A.java
public class A {
    public String getS4ClassRepresentation() { return ""; }
}

// FILE: main.kt
fun foo(a: A) {
    a.s4ClassRepresentation.length
}
