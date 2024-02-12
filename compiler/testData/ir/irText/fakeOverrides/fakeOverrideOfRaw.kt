// ISSUE: KT-65298
// TARGET_BACKEND: JVM
// SEPARATE_SIGNATURE_DUMP_FOR_K2
// FILE: Java1.java

public class Java1 {
    public void foo(List a) {};
    public List bar() { return null; };
}

// FILE: 1.kt
class A : Java1() {
}