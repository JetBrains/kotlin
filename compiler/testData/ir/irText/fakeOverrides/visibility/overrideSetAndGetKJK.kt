// SKIP_KT_DUMP
// TARGET_BACKEND: JVM

// SEPARATE_SIGNATURE_DUMP_FOR_K2
// REASON: KT-70036 Fir2IR Lazy IR does not contain parameter name for fake setters of a1 and a2

// FILE: J.java
public class J extends A {
    @Override public int getA1() { return 1; }
    @Override protected int getA2() { return 1; }
    @Override public void setA1(int a) { }
    @Override protected void setA2(int a) { }
}


// FILE: test.kt
abstract class A {
    public var a1 = 0
    protected var a2 = 0
}

class B: J() {}

fun test(b: B) {
    b.a1 = 1
}
