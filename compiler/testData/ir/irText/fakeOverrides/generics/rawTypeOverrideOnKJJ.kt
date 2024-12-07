// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// WITH_STDLIB

// FILE: Java1.java
import java.util.*;

public class Java1 {
    public List a = new ArrayList();
    public void foo(List a) { };
    public List bar() {
        return null;
    };
}

// FILE: Java2.java
public class Java2 extends Java1  { }

// FILE: Java3.java
import java.util.*;

public class Java3 extends Java1 {
    public List<Object> a = new ArrayList<Object>(1);
    @Override
    public void foo(List a) { }
    @Override
    public List<Object> bar() {
        return null;
    }
}

// FILE: 1.kt
class A : Java2()   //Kotlin ← Java1 ← Java2

class B : Java2() {
    val a: MutableList<Any?>
        get() = mutableListOf(1)
    override fun bar(): MutableList<Any?> {
        return mutableListOf(2)
    }
    override fun foo(a: MutableList<Any?>?) { }
}

class C : Java3()   // Kotlin ← Java1(override) ← Java2

class D : Java3() {
    override fun foo(a: MutableList<Any?>) { }
    override fun bar(): MutableList<Any?> {
        return mutableListOf(2)
    }
    val a: MutableList<Any?>
        get() = mutableListOf(2)
}

fun test(a: A, b: B, c: C, d: D) {
    a.foo(null)
    a.foo(mutableListOf(null))
    val k: List<Any?> = a.bar()
    b.foo(mutableListOf(null))
    b.foo(null)
    val k2: List<Any?>? = b.bar()
    c.foo(listOf(null))
    c.foo(null)
    val k3: List<Any?> = c.bar()
    d.foo(mutableListOf(null))
    val k4: List<Any?> = d.bar()
}