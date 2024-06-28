// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB

// MODULE: separate

// FILE: Java2.java
import java.util.*

public class Java2<T> {
    public List<T> a = new ArrayList();
    public Queue<T> b = new ArrayDeque();
    public Set<T> c = new HashSet();

    public void foo(List<T> a) {};
    public List<T> bar() { return a; };

    public void foo2(Queue<T> b) {};
    public Queue<T> bar2() { return b; };

    public void foo3(Set<T> c) {};
    public Set<T> bar3() { return c; };
}

// MODULE: main(separate)

// FILE: Java1.java
import java.util.*;

public class Java1<T> {
    public List<T> a = new ArrayList();
    public Queue<T> b = new ArrayDeque();
    public Set<T> c = new HashSet();

    public void foo(List<T> a) {};
    public List<T> bar() { return a; };

    public void foo2(Queue<T> b) {};
    public Queue<T> bar2() { return b; };

    public void foo3(Set<T> c) {};
    public Set<T> bar3() { return c; };
}

// FILE: 1.kt

import java.util.*

class A : Java1<Int>()

class B : Java1<String?>()

class C : Java2<Any>()

class D : Java1<Int>() {
    override fun bar(): MutableList<Int> {
        return null!!
    }
    override fun foo(a: MutableList<Int>) { }
}

class E : Java1<String?>() {
    override fun foo3(c: MutableSet<String?>) { }
    override fun bar3(): MutableSet<String> {
        return null!!
    }
}

class F : Java1<Any>() {
    override fun bar2(): Queue<Any> {
        return null!!
    }
    override fun foo2(b: Queue<Any>?) { }
}

fun test(a: A, b: B, c: C, d: D, e: E, f: F) {
    val k: MutableList<Int> = a.a
    val k2: Queue<Int> = a.b
    val k3: MutableSet<Int> = a.c
    a.a = mutableListOf<Int>()
    a.b = LinkedList<Int>()
    a.c = mutableSetOf<Int>()
    val k4: MutableList<Int> = a.bar()
    val k5: Queue<Int> = a.bar2()
    val k6: MutableSet<Int> = A().bar3()
    a.foo(k)
    a.foo2(k2)
    a.foo3(k3)

    val k19: MutableList<String?> = b.a
    val k20: Queue<String?> = b.b
    val k21: MutableSet<String?> = b.c
    b.a = mutableListOf<String?>()
    b.b = LinkedList<String>()
    b.c = mutableSetOf<String?>()
    val k22: MutableList<String?> = b.bar()
    val k23: Queue<String?> = b.bar2()
    val k24: MutableSet<String?> = b.bar3()
    b.foo(k19)
    b.foo2(k20)
    b.foo3(k21)

    val k25: MutableList<Any> = c.a
    val k26: Queue<Any> = c.b
    val k27: MutableSet<Any> = c.c
    c.a = mutableListOf<Any>()
    c.b = LinkedList<Any>()
    c.c = mutableSetOf<Any>()
    val k28: MutableList<Any> = c.bar()
    val k29: Queue<Any> = c.bar2()
    val k30: MutableSet<Any> = c.bar3()
    c.foo(k25)
    c.foo2(k26)
    c.foo3(k27)

    val k31: MutableList<Int> = d.bar()
    e.foo3(mutableSetOf(null, ""))
    val k32 : Queue<Any> = f.bar2()
    f.foo2(LinkedList<Any>())
}
