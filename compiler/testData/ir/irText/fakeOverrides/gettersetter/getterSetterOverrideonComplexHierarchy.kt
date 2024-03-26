// SKIP_KT_DUMP
// TARGET_BACKEND: JVM

// FILE: Java1.java
public interface Java1  {
    Object getA();
    boolean isB();
}

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
public interface Java3  {
    Object a = "1";
    Object getA();
    void setA(Object t);

    boolean b = true;
    boolean isB();
}

// FILE: Java4.java
public interface Java4 extends KotlinInterface {}

// FILE: Java5.java
public interface Java5  extends Java3 { }

// FILE: 1.kt
abstract class A : Java1, Java2(), KotlinInterface // Kotlin ← Java1, Java2, Kotlin2

class B :  Java1, Java2(), KotlinInterface {
    override var b: Boolean = false
        get() = true
}

class C(override var b: Boolean) : Java2(), KotlinInterface, KotlinInterface2 { // Kotlin ← Java, Kotlin1, Kotlin2
    override fun isB(): Boolean {
        return true
    }
}

abstract class D : Java1, Java2(), Java3    //Kotlin ← Java1, Java2, Java3

class E : Java1, Java2(), Java3 {
    override fun setA(t: Any?) { }
}

abstract class F : Java2(), Java4    //Kotlin ← Java1, Java2 ← Kotlin2

class G(override var b: Boolean) : Java2(), Java4 {
    override fun getA(): Int {
        return 10
    }
}

abstract class H : Java4, KotlinInterface2 // Kotlin ← Java, Kotlin2 ← Kotlin3

class I : Java4, KotlinInterface2{
    override var b: Boolean = false
        get() = true
        set(value) {
            field = false
        }
}

abstract class J : KotlinInterface3, Java2()    //Kotlin ← Java, Kotlin2 ← Java2

class K : KotlinInterface3, Java2() {
    override fun setA(t: Any?) {}
}

abstract class L : Java2(), Java5   //Kotlin ← Java1, Java2 ← Java3

class M : Java2(), Java5 {
    override fun setA(t: Any) { }
}


interface KotlinInterface {
    var b: Boolean
}

interface KotlinInterface2 {
    fun isB(): Boolean {
        return true
    }
}

interface KotlinInterface3 : Java3

fun test(a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M){
    a.a
    a.isB
    b.a
    b.a = 2
    b.isB
    c.a
    c.isB
    c.a = 2
    c.b = false
    d.a
    d.b
    d.isB
    e.a
    e.b
    e.isB
    f.a
    f.isB
    f.a = 2
    g.a
    g.isB
    g.a = 2
    h.b
    h.isB()
    h.b = false
    i.b
    i.isB()
    i.b = false
    j.a
    j.isB
    k.a
    k.isB
    l.a
    l.isB
    m.a
    m.isB
}