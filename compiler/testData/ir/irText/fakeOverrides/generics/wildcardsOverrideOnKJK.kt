// SKIP_KT_DUMP
// FIR_IDENTICAL
// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB

// FILE: Java1.java
import java.util.*;

public class Java1 extends KotlinClass {
    @Override
    public void foo(ArrayList<? extends Number> a) { }
    @Override
    public ArrayList<? extends Number> bar(){
        return null;
    }
    @Override
    public void foo2(ArrayList<? super Number> a) { }

    @Override
    public ArrayList<? super Number> bar2(){
        return null;
    }
    @Override
    public void foo3(List<?> a) {}
    @Override
    public List<?> bar3(){
        return null;
    }
}

// FILE: Java2.java
public class Java2 extends KotlinClass  { }

// FILE: 1.kt
class A: Java2()    // Kotlin ← Java ← Kotlin

class B : Java2() {
    override fun foo(a: ArrayList<out Number>){ }
    override fun bar(): ArrayList<out Number> {
        return arrayListOf(2)
    }
    override fun foo2(a: ArrayList<in Number>) { }
    override fun bar2(): ArrayList<in Number> {
        return arrayListOf(2)
    }
    override fun foo3(a: List<*>) { }
    override fun bar3(): List<*> {
        return arrayListOf("2")
    }
}

class C : Java1()   // Kotlin ← Java(override) ← Kotlin

class D : Java1() {
    override fun foo(a: ArrayList<out Number>) { }
    override fun bar(): ArrayList<out Number> {
        return arrayListOf(3)
    }
}

open class KotlinClass {
    open fun foo(a: ArrayList<out Number>) { }
    open fun bar(): ArrayList<out Number> {
        return arrayListOf(1)
    }

    open fun foo2(a: ArrayList<in Number>) {}
    open fun bar2(): ArrayList<in Number> {
        return arrayListOf(1)
    }

    open fun foo3(a: List<*>) {}
    open fun bar3(): List<*> {
        return arrayListOf(1)
    }
}

fun test(a: A, b: B, c: C, d: D) {
    a.foo(arrayListOf(1))
    a.bar()
    a.foo2(arrayListOf(1))
    a.bar2()
    a.foo3(listOf(null))
    a.foo3(listOf(1))
    a.foo3(mutableListOf(""))
    a.bar3()
    b.foo(arrayListOf(1))
    b.bar()
    b.foo2(arrayListOf(1))
    b.bar2()
    b.foo3(listOf(null))
    b.foo3(listOf(1))
    b.foo3(mutableListOf(""))
    b.bar3()
    c.foo(arrayListOf(1))
    c.bar()
    c.foo2(arrayListOf(1))
    c.bar2()
    c.foo3(listOf(null))
    c.foo3(listOf(1))
    c.foo3(mutableListOf(""))
    c.bar3()
    d.foo(arrayListOf(1))
    d.bar()
}