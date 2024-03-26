// SKIP_KT_DUMP
// FIR_IDENTICAL
// TARGET_BACKEND: JVM

// FILE: Java1.java
public class Java1 {
    private int a;
    public int getA() {
        return 120;
    }
    public void setA(int t) {
        a = 100;
    }

    private boolean b = true;
    public boolean isB() {
        return b;
    }
    public void setB(boolean t) {
        b = false;
    }

    private String c;
    public String getC() {
        return c;
    }

    private Integer d;
    public void setD(Integer t) {
        d = t;
    }
}

// FILE: 1.kt
class A : Java1()

class B : Java1() {
    override fun getA(): Int {
        return 12
    }
    override fun setA(t: Int) {
        a = 10
    }
    override fun isB(): Boolean {
        return false
    }
}

fun test(a: A, b: B) {
    val k: Int = a.a
    a.a = 3
    val k2: Boolean = a.isB
    a.isB = false
    val k3: String = a.c

    val k4: Int = b.a
    b.a = 3
    val k5: Boolean = b.isB
    b.isB = false
    val k6: String = b.c
}