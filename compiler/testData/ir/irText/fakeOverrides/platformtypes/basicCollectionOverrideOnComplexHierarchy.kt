// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// WITH_STDLIB

// FILE: Java1.java

public class Java1 extends A { }

// FILE: Java2.java
import java.util.*;

public interface Java2 {
    List<Integer> a = new ArrayList();
    Queue<String> b = new ArrayDeque();
    Set<Object> c = new HashSet();
    public int[] d = new int[1];
    public String[] e = new String[1];

    void foo(List<Integer> a);
    List<Integer> bar();

    void foo2(Set<Object> c);
    Set<Object> bar2();

    public void foo3(int[] d);
    public int[] bar3();

    public void foo4(String[] e);
    public String[] bar4();
}

// FILE: Java3.java
public interface Java3 extends KotlinInterface { }

// FILE: 1.kt
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

open class A {
    open var a : ArrayList<Int> = arrayListOf()
    open var b : HashSet<Any?> = hashSetOf()
    open var c : IntArray = intArrayOf()
    open var d : Array<out String> = arrayOf()

    open fun foo(a: ArrayList<Int>) {}
    open  fun bar(): ArrayList<Int> { return a }

    open fun foo2(b: HashSet<Any?>) {}
    open fun bar2():HashSet<Any?> { return b }

    open fun foo3(c : IntArray) {}
    open fun bar3(): IntArray { return c }

    open fun foo4(d: Array<out String>) {}
    open fun bar4(): Array<out String>{ return d }
}

class B : Java1()       //Kotlin ← Java ← Kotlin

class C : Java1() {     //Kotlin ← Java ← Kotlin with explicit override
    override var a: ArrayList<Int>
        get() = arrayListOf()
        set(value) {}
    override fun bar(): ArrayList<Int> {
        return arrayListOf<Int>()
    }
    override fun foo(a: ArrayList<Int>) { }

    override var b: HashSet<Any?>
        get() = hashSetOf()
        set(value) {}
    override fun bar2(): HashSet<Any?> {
        return hashSetOf()
    }
    override fun foo2(b: HashSet<Any?>) { }

    override var c: IntArray
        get() = intArrayOf()
        set(value) {}

    override fun bar3(): IntArray {
        return intArrayOf()
    }
    override fun foo3(c: IntArray) { }

    override var d: Array<out String>
        get() = arrayOf()
        set(value) {}
    override fun bar4(): Array<out String> {
        return arrayOf()
    }
    override fun foo4(d: Array<out String>) { }
}

abstract class D : Java3    //Kotlin ← Java ← Kotlin ← Java

class E : Java3 {   //Kotlin ← Java ← Kotlin ← Java with explicit override
    override fun foo(a: MutableList<Int>?) { }
    override fun bar(): MutableList<Int> {
        return mutableListOf()
    }

    override fun foo2(c: MutableSet<Any>?) { }
    override fun bar2(): MutableSet<Any> {
        return mutableSetOf()
    }

    override fun foo3(d: IntArray?) { }
    override fun bar3(): IntArray {
        return intArrayOf()
    }

    override fun foo4(e: Array<out String>?) { }
    override fun bar4(): Array<String> {
        return arrayOf("")
    }
}

interface KotlinInterface : Java2

fun test(b: B, c: C, d: D, e: E) {
    b.a = arrayListOf(1)
    b.b = hashSetOf(1, "", null)
    b.c = intArrayOf(1)
    b.d = arrayOf("")
    b.foo(arrayListOf(1))
    b.foo2(hashSetOf(1, "", null))
    b.foo3(intArrayOf(1))
    b.foo4(arrayOf(""))
    val k1: List<Int> = b.bar()
    val k2: Set<Any?> = b.bar2()
    val k3: IntArray = b.bar3()
    val k4: Array<out String> = b.bar4()

    c.a = arrayListOf(1)
    c.b = hashSetOf(1, "", null)
    c.c = intArrayOf(1)
    c.d = arrayOf("")
    c.foo(arrayListOf(1))
    c.foo2(hashSetOf(1, "", null))
    c.foo3(intArrayOf(1))
    c.foo4(arrayOf(""))
    val k5: List<Int> = b.bar()
    val k6: Set<Any?> = b.bar2()
    val k7: IntArray = b.bar3()
    val k8: Array<out String> = b.bar4()

    d.foo(arrayListOf(1))
    d.foo2(setOf(1))
    d.foo3(intArrayOf(1))
    d.foo4(arrayOf(""))
    val k9: List<Int> = d.bar()
    val k10: Set<Any?> = d.bar2()
    val k11: IntArray = d.bar3()
    val k12: Array<out String> = d.bar4()

    e.foo(arrayListOf(1))
    e.foo2(mutableSetOf(1, ""))
    e.foo3(intArrayOf(1))
    e.foo4(arrayOf(""))
    val k13: List<Int> = e.bar()
    val k14: Set<Any?> = e.bar2()
    val k15: IntArray = e.bar3()
    val k16: Array<out String> = e.bar4()
}