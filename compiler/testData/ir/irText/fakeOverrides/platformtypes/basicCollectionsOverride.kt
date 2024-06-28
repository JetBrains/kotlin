// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB

// MODULE: separate

// FILE: Java2.java
import java.util.*;

public class Java2 {
    public List<Integer> a = new ArrayList();
    public Queue<String> b = new ArrayDeque();
    public Set<Object> c = new HashSet();
    public int[] d = new int[1];
    public String[] e = new String[1];

    public void foo(List<Integer> a) {};
    public List<Integer> bar() { return a; };

    public void foo2(Queue<String> b) {};
    public Queue<String> bar2() { return b; };

    public void foo3(Set<Object> c) {};
    public Set<Object> bar3() { return c; };

    public void foo4(int[] d) {};
    public int[] bar4() { return d; };

    public void foo5(String[] e){};
    public String[] bar5() { return e; };
}

// MODULE: main(separate)

// FILE: Java1.java
import java.util.*;

public class Java1 {
    public List<Integer> a = new ArrayList();
    public Queue<String> b = new ArrayDeque();
    public Set<Object> c = new HashSet();
    public int[] d = new int[1];
    public String[] e = new String[1];

    public void foo(List<Integer> a) {};
    public List<Integer> bar() { return a; };

    public void foo2(Queue<String> b) {};
    public Queue<String> bar2() { return b; };

    public void foo3(Set<Object> c) {};
    public Set<Object> bar3() { return c; };

    public void foo4(int[] d) {};
    public int[] bar4() { return d; };

    public void foo5(String[] e){};
    public String[] bar5() { return e; };
}

// FILE: Java3.java
public interface Java3 extends KotlinInterface { }

// FILE: 1.kt
import java.util.*

class A : Java1()   // Kotlin ← Java

class B : Java2()   // Kotlin ← Java (separate module)

class C : Java1() { // Kotlin ← Java with explicit override
    override fun bar(): MutableList<Int> {
        return mutableListOf()
    }
    override fun bar2(): Queue<String> {
        return LinkedList()
    }
    override fun bar3(): MutableSet<Any> {
        return mutableSetOf()
    }
    override fun bar4(): IntArray {
        return intArrayOf()
    }
    override fun bar5(): Array<String> {
        return arrayOf("")
    }
    override fun foo(a: MutableList<Int>) { }
    override fun foo2(b: Queue<String>) { }
    override fun foo3(c: MutableSet<Any>) { }
    override fun foo4(d: IntArray) { }
    override fun foo5(e: Array<out String>) { }
}

fun test(a: A, b: B, c: C) {
    a.a = listOf(1)
    a.b = LinkedList()
    a.c = setOf(null, 1)
    a.d = intArrayOf(1)
    a.e = arrayOf(null)
    a.foo(null)
    a.foo(listOf(1,2))
    a.foo2(null)
    a.foo2(LinkedList())
    a.foo3(null)
    a.foo3(setOf("", 1))
    a.foo4(null)
    a.foo4(intArrayOf(1))
    a.foo5(null)
    a.foo5(arrayOf(""))
    val k: List<Int> = a.bar()
    val k2: Queue<String> = a.bar2()
    val k3: Set<Any> = a.bar3()
    val k4: IntArray = a.bar4()
    val k5: Array<String> = a.bar5()

    b.a = listOf(1)
    b.b = LinkedList()
    b.c = setOf(null, 1)
    b.d = intArrayOf(1)
    b.e = arrayOf("")
    b.foo(null)
    b.foo(listOf(1,2))
    b.foo2(null)
    b.foo2(LinkedList())
    b.foo3(null)
    b.foo3(setOf("", 1))
    b.foo4(null)
    b.foo4(intArrayOf(1))
    b.foo5(null)
    b.foo5(arrayOf(""))
    val k6: List<Int> = b.bar()
    val k7: Queue<String> = b.bar2()
    val k8: Set<Any> = b.bar3()
    val k9: IntArray = b.bar4()
    val k10: Array<String> = b.bar5()

    c.a = listOf(1)
    c.b = LinkedList()
    c.c = setOf(null, 1)
    c.d = intArrayOf(1)
    c.e = arrayOf("")
    c.foo(mutableListOf())
    c.foo2(LinkedList())
    c.foo3(mutableSetOf())
    c.foo4(intArrayOf(1))
    c.foo5(arrayOf(""))
    val k11: List<Int> = c.bar()
    val k12: Queue<String> = c.bar2()
    val k13: Set<Any> = c.bar3()
    val k14: IntArray = c.bar4()
    val k15: Array<String> = c.bar5()
}
