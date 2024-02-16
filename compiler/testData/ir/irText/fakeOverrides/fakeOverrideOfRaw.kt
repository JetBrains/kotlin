// FIR_IDENTICAL
// ISSUE: KT-65298
// TARGET_BACKEND: JVM
// FILE: Java1.java

public class Java1 {
    public void foo(List a) {};
    public List bar() { return null; };
}

// FILE: 1.kt
class A : Java1() {
}
