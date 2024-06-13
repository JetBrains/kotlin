// FIR_IDENTICAL
// SKIP_KT_DUMP
// TARGET_BACKEND: JVM

// FILE: Java1.java
public class Java1 extends Java2  { }

// FILE: Java2.java
public class Java2  {
    private int a = 0;
    public int getA(){
        return 1;
    }
    public void setA(int t){
        a = t;
    }

    protected boolean b = true;
    public boolean isB(){
        return b;
    }
}

// FILE: Java3.java
public class Java3 extends Java2 {
    @Override
    public void setA(int t){ }
    public void setB(boolean t){
        this.b = t;
    };
}

// FILE: 1.kt
class A : Java1()

class B : Java1() {
    override fun getA(): Int {
        return 100
    }
    fun setB(t: Boolean) {
        b = t
    }
    override fun isB(): Boolean {
        return false
    }
}

class C : Java3()

class D : Java3() {
    override fun setA(t: Int) {}
    override fun setB(t: Boolean) {}
}

fun test(a: A, b: B, c: C, d: D){
    a.a
    a.a = 2
    a.isB
    b.a
    b.a = 2
    b.isB
    b.isB = true
    c.a
    c.a = 2
    c.isB
    c.isB = true
    d.a
    d.a = 2
    d.isB
    d.isB = true
}