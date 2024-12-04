// SKIP_KT_DUMP
// FIR_IDENTICAL
// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB

// FILE: Java1.java
import java.util.*;
public class Java1 {
    public void foo(List<? extends Number> a) { }
    public List<? extends Number> bar(){
        return null;
    }
    public void foo2(List<? super Number> a) { }
    public List<? super Number> bar2(){
        return null;
    }

    public void foo3(List<?> a) {}
    public List<?> bar3(){
        return null;
    }
}

// FILE: Java2.java
public interface Java2 extends KotlinInterface { }

// FILE: Java3.java
import java.util.List;

public interface Java3  {
    public List<Object> bar();
    public List<Object> bar2();
    public List<Object> bar3();
}

// FILE: Java4.java
public class Java4 extends Java1 { }

// FILE: Java5.java
public class Java5 extends KotlinClass { }


// FILE: 1.kt
class A: Java1(), Java2 // Kotlin ← Java1, Java2 ← Kotlin2

class B : Java1(), Java2 {
    override fun bar2(): MutableList<in Number> {
        return mutableListOf(2)
    }
    override fun foo2(a: MutableList<in Number>) { }
}

abstract class C : Java2, KotlinInterface2  // Kotlin ← Java, Kotlin2 ← Kotlin3

class D : Java2, KotlinInterface2 {
    override fun bar(): MutableList<Int> {
        return mutableListOf(2)
    }
    override fun bar2(): MutableList<Any> {
        return mutableListOf(2)
    }
    override fun bar3(): MutableList<Int> {
        return mutableListOf(2)
    }

    override fun foo2(a: MutableList<in Number>) {}
    override fun foo(a: MutableList<out Number>) {}
    override fun foo3(a: MutableList<*>) { }
}

class E : KotlinClass(), Java3  //Kotlin ← Java, Kotlin2 ← Java2

class F : KotlinClass(), Java3 {
    override fun foo(a: MutableList<out Number>) { }
    override fun foo2(a: MutableList<in Number>) { }
    override fun foo3(a: MutableList<*>) { }
}

class G : Java4(), Java2    //Kotlin ← Java1, Java2 ← Java3

class H : Java4(), Java2 {
    override fun bar(): MutableList<out Number> {
        return mutableListOf(5)
    }
    override fun bar2(): MutableList<in Number> {
        return mutableListOf(5)
    }
    override fun bar3(): MutableList<*> {
        return mutableListOf(5)
    }
}

class I : Java5()   //Kotlin ← Java ← Kotlin ← Java

class J : Java5() {
    override fun foo(a: MutableList<out Number>?) { }
    override fun bar(): MutableList<out Number> {
        return mutableListOf(6)
    }
}

interface KotlinInterface {
    fun foo(a: MutableList<out Number>)
    fun bar(): MutableList<out Number>

    fun foo2(a: MutableList<in Number>)
    fun bar2(): MutableList<in Number>

    fun foo3(a: MutableList<*>)
    fun bar3(): MutableList<*>
}

interface KotlinInterface2 {
    fun bar(): MutableList<Int>
    fun bar2(): MutableList<Any>
    fun bar3(): MutableList<Int>
}

open class KotlinClass : Java1()

fun test(a:A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J){
    a.foo(mutableListOf(1))
    a.foo(null)
    a.bar()
    a.foo2(mutableListOf(1))
    a.foo2(null)
    a.bar2()
    a.foo3(mutableListOf(1))
    a.foo3(null)
    a.bar3()


    b.foo2(mutableListOf(1))
    b.foo2(mutableListOf(null))
    b.bar2()

    c.foo(mutableListOf(1))
    c.bar()
    c.foo2(mutableListOf(1.1))
    c.bar2()
    c.foo3(mutableListOf(1))
    c.bar3()

    d.foo(mutableListOf(1))
    d.bar()
    d.foo2(mutableListOf(1))
    d.bar2()
    d.foo3(mutableListOf(1))
    d.bar3()

    e.foo(mutableListOf(1))
    e.bar()
    e.foo2(mutableListOf(1))
    e.bar2()
    e.foo3(mutableListOf(1))
    e.bar3()

    f.foo(mutableListOf(1))
    f.foo2(mutableListOf(1))
    f.foo3(mutableListOf(1))

    g.foo(mutableListOf(1))
    g.bar()
    g.foo2(mutableListOf(1))
    g.bar2()
    g.foo3(mutableListOf(1))
    g.bar3()

    h.foo(mutableListOf(1))
    h.bar()
    h.foo2(mutableListOf(1))
    h.bar2()
    h.foo3(mutableListOf(1))
    h.bar3()

    i.foo(mutableListOf(1))
    i.bar()
    i.foo2(mutableListOf(1))
    i.bar2()
    i.foo3(mutableListOf(1))
    i.bar3()

    j.foo(mutableListOf(1))
    j.bar()
    j.foo2(mutableListOf(1))
    j.bar2()
    j.foo3(mutableListOf(1))
    j.bar3()
}