// SKIP_KT_DUMP
// TARGET_BACKEND: JVM

// FILE: Java1.java
public class Java1 extends KotlinClass {}

// FILE: Java2.java
public class Java2  {
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

// FILE: Java3.java
public class Java3 extends KotlinClass2 { }

// FILE: 1.kt
class A : Java1()

class B : Java1() {
    override fun getA(): Int {
        return 2
    }
    override fun setA(t: Int) { }
    override fun isB(): Boolean {
        return false
    }
    override fun setB(t: Boolean) {}
}
class C : Java3()

open class KotlinClass : Java2()

open class KotlinClass2 : Java2() {
    val b = true
}

fun test(a: A, b: B, c: C) {
    val k1: Int = a.a
    val k2: Boolean = a.isB
    a.a = 2
    a.isB = true

    val k3: Int = b.a
    val k4: Boolean = b.isB
    b.a = 2
    b.isB = false

    val k5: Boolean = c.b
    val k6: Boolean = c.isB
    c.isB = true
    val k7: Int = c.a
    c.a = 2
}