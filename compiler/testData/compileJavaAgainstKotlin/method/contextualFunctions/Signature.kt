// LANGUAGE: +ContextParameters
// TARGET_BACKEND: JVM_IR

// FILE: Signature.java
package test;

class JavaClass {
    public static void test(C1 c1, C2 c2, R r, P1 p1, P2 p2) {
        SignatureKt.f(c1, c2, r, p1, p2);
    }
}

// FILE: Signature.kt
package test

interface C1
interface C2
interface R
interface P1
interface P2

context(_: C1, _: C2)
fun R.f(p1: P1, p2: P2) {}
