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
import java.util.List;

public interface Java2  {
    public List<Integer> bar();
    public List<Number> bar2();
    public List<Integer> bar3();
}

// FILE: Java3.java
import java.util.List;

public interface Java3  {
    public List<? extends Object> bar();
    public void foo3(List<?> a);
    public List<?> bar3();
}

// FILE: 1.kt
class A : Java1(), Java2 {   //Kotlin ← Java1, Java2
    override fun bar(): MutableList<Int> {
        return mutableListOf(1)
    }
    override fun bar2(): MutableList<Number> {
        return mutableListOf(1.1)
    }
    override fun bar3(): MutableList<Int> {
        return mutableListOf(1)
    }
}

class B : Java1() , KotlinInterface //Kotlin ← Java, Kotlin2

class C : Java1() , KotlinInterface {
    override fun foo(a: MutableList<out Number>) { }
    override fun bar(): MutableList<out Number> {
        return mutableListOf(1)
    }
}

class D : Java1(), Java2, KotlinInterface {     //Kotlin ← Java1, Java2, Kotlin2
    override fun bar(): MutableList<Int> {
        return mutableListOf(1)
    }
    override fun bar2(): MutableList<Number> {
        return mutableListOf(1.1)
    }
    override fun bar3(): MutableList<Int> {
        return mutableListOf(1)
    }
}

class E : Java1(), KotlinInterface, KotlinInterface2   //Kotlin ← Java, Kotlin1, Kotlin2

class F : Java1(), KotlinInterface, KotlinInterface2 {
    override fun bar2(): MutableList<in Number> {
        return mutableListOf(1)
    }
    override fun foo2(a: MutableList<in Number>) { }
}

abstract class G : Java1(), Java2, Java3 {  //Kotlin ← Java1, Java2, Java3
    override fun bar(): MutableList<Int> {
        return mutableListOf(1)
    }
    override fun bar2(): MutableList<Number> {
        return mutableListOf(1.1)
    }
    override fun bar3(): MutableList<Int> {
        return mutableListOf(1)
    }
}

class H : G() {
    override fun foo(a: MutableList<out Number>) { }
    override fun foo2(a: MutableList<in Number>) { }
    override fun foo3(a: MutableList<*>) { }
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
    fun foo(a: List<Number>)
    fun bar(): List<Number>
}

fun test(a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H){
    a.foo(null)
    a.foo(mutableListOf(1))
    a.foo(listOf(1))
    a.bar()
    a.foo2(null)
    a.foo2(mutableListOf(null))
    a.foo2(mutableListOf(1.1))
    a.bar2()
    a.foo3(null)
    a.foo3(mutableListOf(null))
    a.foo3(listOf(null))
    a.foo3(listOf(""))
    a.bar3()

    b.foo(null)
    b.foo(mutableListOf(1))
    b.bar()
    b.foo2(null)
    b.foo2(mutableListOf(null))
    b.foo2(mutableListOf(1.1))
    b.bar2()
    b.foo3(null)
    b.foo3(mutableListOf(null))
    b.foo3(mutableListOf(""))
    b.bar3()

    c.foo(mutableListOf(1))
    c.bar()

    d.foo(null)
    d.foo(mutableListOf(1))
    d.bar()
    d.foo2(null)
    d.foo2(mutableListOf(null))
    d.foo2(mutableListOf(1.1))
    d.bar2()
    d.foo3(null)
    d.foo3(mutableListOf(null))
    d.foo3(mutableListOf(""))
    d.bar3()

    e.foo(null)
    e.foo(mutableListOf(1))
    e.foo(listOf(1))
    e.bar()
    e.foo2(null)
    e.foo2(mutableListOf(null))
    e.foo2(mutableListOf(1.1))
    e.bar2()
    e.foo3(null)
    e.foo3(mutableListOf(null))
    e.foo3(listOf(null))
    e.foo3(listOf(""))
    e.bar3()

    f.foo2(mutableListOf(null))
    f.bar2()

    g.foo(null)
    g.foo(mutableListOf(1))
    g.foo(listOf(1))
    g.bar()
    g.foo2(null)
    g.foo2(mutableListOf(null))
    g.foo2(mutableListOf(1.1))
    g.bar2()
    g.foo3(null)
    g.foo3(mutableListOf(null))
    g.foo3(listOf(null))
    g.foo3(listOf(""))
    g.bar3()

    h.foo(mutableListOf(1))
    h.foo2(mutableListOf(1))
    h.foo3(mutableListOf(""))
}