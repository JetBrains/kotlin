// SKIP_KT_DUMP
// FIR_IDENTICAL
// TARGET_BACKEND: JVM

// FILE: Java1.java
public class Java1  {
    private int a = 0;

    public int getA(){
        return 1;
    }
    public void setA(int t){
        a = 100;
    };

    private boolean b = true;
    public boolean isB(){
        return b;
    };
    public void setB(boolean t){
        b = t;
    };
}

// FILE: Java2.java
public interface Java2  {
    Object a = "1";
    Object getA();
    void setA(Object t);

    boolean b = true;
    boolean isB();
}

// FILE: 1.kt

abstract class A : Java1(), Java2

class B : Java1(), Java2 {
    override fun setA(t: Any?) { }
}

abstract class C : Java1(), KotlinInterface

class D(override var a: Any, override val b: Boolean) : Java1(), KotlinInterface

interface KotlinInterface {
    var a : Any
    val b : Boolean
}
fun test(a: A, b: B, c: C, d: D){
    a.a
    a.setA(3)
    a.setA("")
    a.isB
    a.isB = false
    b.a
    b.setA(3)
    b.setA("")
    b.isB
    b.isB = false
    c.a
    c.b
    c.isB
    c.a = 1
    c.isB = false
    d.a
    d.b
    d.isB
    d.a = 1
    d.isB = false
}