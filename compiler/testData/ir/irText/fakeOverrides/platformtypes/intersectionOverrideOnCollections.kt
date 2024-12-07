// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// WITH_STDLIB

// FILE: Java1.java
import java.util.*;

public interface Java1 {
    public List<Integer> a = new ArrayList();
    public void foo(List<Integer> a);
    public List<Integer> bar();

    public int[] d = new int[1];
    public void foo2(int[] a);
    public int[] bar2();

    public String[] e = new String[1];
    public void foo3(String[] a);
    public String[] bar3();
}

// FILE: Java2.java
import java.util.*;

public interface Java2 {
    public List<Integer> a = new ArrayList();
    public void foo(ArrayList<Integer> a);
    public ArrayList<Integer> bar();

    public int[] d = new int[1];
    public void foo2(int[] a);
    public int[] bar2();

    public String[] e = new String[1];
    public void foo3(String[] a);
    public String[] bar3();
}

// FILE: 1.kt
import kotlin.collections.ArrayList


interface A : Java1, Java2  //Kotlin ← Java1, Java2

abstract class B: Java1, Java2 {    //Kotlin ← Java1, Java2 with explicit override
    override fun foo(a: MutableList<Int>) { }
    override fun bar(): ArrayList<Int> {
        return null!!
    }

    override fun foo2(a: IntArray?) { }
    override fun bar2(): IntArray {
        return null!!
    }

    override fun foo3(a: Array<out String>?) { }
    override fun bar3(): Array<String> {
        return null!!
    }
}

interface C : Java2, KotlinInterface //Kotlin ← Java, Kotlin2

abstract class D : Java2, KotlinInterface { //Kotlin ← Java, Kotlin2 with explicit override
    override fun foo(o: ArrayList<Int>) { }
    override fun bar(): ArrayList<Int> {
        return null!!
    }

    override fun foo2(a: IntArray?) { }
    override fun bar2(): IntArray {
        return null!!
    }

    override fun foo3(a: Array<out String>?) { }
    override fun bar3(): Array<String> {
        return null!!
    }
}

interface KotlinInterface {
    fun foo(o: ArrayList<Int>)
    fun bar(): ArrayList<Int>
}


fun test(a: A, b: B, c: C, d: D){
    a.foo(listOf(1,null))
    a.foo(arrayListOf(1, null))
    a.foo2(null)
    a.foo2(intArrayOf())
    a.foo3(null)
    a.foo3(arrayOf(null))
    val k: MutableList<Int> = a.bar()
    val k2: ArrayList<Int> = a.bar()
    val k3: IntArray = a.bar2()
    val k4: Array<String> = a.bar3()

    b.foo(mutableListOf(1))
    b.foo(arrayListOf(1, null))
    b.foo2(null)
    b.foo2(intArrayOf())
    b.foo3(null)
    b.foo3(arrayOf(""))
    val k5: MutableList<Int> = b.bar()
    val k6: ArrayList<Int> = b.bar()
    val k7: IntArray = b.bar2()
    val k8: Array<String> = b.bar3()

    c.foo(arrayListOf(1, null))
    c.foo(null)
    c.foo2(intArrayOf(1))
    c.foo2(null)
    c.foo3(arrayOf(null))
    c.foo3(null)
    val k9: MutableList<Int> = c.bar()
    val k10: ArrayList<Int> = c.bar()
    val k11: IntArray = c.bar2()
    val k12: Array<String> = c.bar3()

    d.foo(arrayListOf(1))
    d.foo2(intArrayOf(1))
    d.foo2(null)
    d.foo3(arrayOf(""))
    d.foo3(null)
    val k13: MutableList<Int> = d.bar()
    val k14: ArrayList<Int> = d.bar()
    val k15: IntArray = d.bar2()
    val k16: Array<String> = d.bar3()
}
