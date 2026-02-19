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
import java.util.*;

public interface Java2  {
    List a = new ArrayList();
    void foo(List a);
    List bar();
}

// FILE: Java3.java
import java.util.*;

public interface Java3  {
    List<Object> a = new ArrayList<Object>(1);
    void foo(List<Object> a);
    List<Object> bar();
}

// FILE: 1.kt
class A : Java1(), Java3    //Kotlin ← Java1, Java2

class B : Java1(), Java3 {
    override fun bar(): MutableList<Any?>? {
        return mutableListOf()
    }
    override fun foo(a: MutableList<Any?>?) { }
}

abstract class C: Java1(), KotlinInterface  //Kotlin ← Java, Kotlin2

class D : Java1(), KotlinInterface {
    override var a: List<Any?>
        get() = emptyList()
        set(value) {}
    override fun foo(a: List<Any?>) { }
    override fun bar(): List<Any?> {
        return null!!
    }
}

abstract class E: Java1(), Java2, KotlinInterface   //Kotlin ← Java1, Java2, Kotlin2

class F(override var a: List<Any?>) : Java1(), Java2, KotlinInterface {
    override fun bar(): MutableList<Any?> {
        return mutableListOf(1)
    }
}

abstract class G : Java1(), KotlinInterface, KotlinInterface2   //Kotlin ← Java, Kotlin1, Kotlin2

class H(override var a: List<Any?>) : Java1(), KotlinInterface, KotlinInterface2 {
    override fun foo(a: List<Any?>) { }
}

class I : Java1(), Java2, Java3 //Kotlin ← Java1, Java2, Java3

class J : Java1(), Java2, Java3 {
    override fun foo(a: MutableList<Any?>) { }
    override fun bar(): MutableList<Any?> {
        return mutableListOf("")
    }
}

interface KotlinInterface {
    var a: List<Any?>
    fun foo(a: List<Any?>)
    fun bar(): List<Any?>
}

interface KotlinInterface2 {
    var a: List<*>
    fun foo(a: List<*>)
    fun bar(): List<*>
}

fun test(a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J) {
    a.foo(null)
    a.foo(mutableListOf(null))
    val k: List<Any?> = a.bar()
    b.foo(mutableListOf(null))
    b.foo(null)
    val k2: List<Any?>? = b.bar()
    c.foo(listOf(null))
    c.foo(null)
    val k3: List<Any?> = c.bar()
    d.foo(listOf(null))
    val k4: List<Any?> = d.bar()
    e.foo(listOf(null))
    val k5: List<Any?> = e.bar()
    f.foo(listOf(null))
    val k6: List<Any?> = f.bar()
    g.foo(listOf(null))
    val k7: List<Any?> = g.bar()
    h.foo(listOf(null))
    val k8: List<Any?> = h.bar()
    i.foo(listOf(null))
    val k9: List<Any?> = i.bar()
    j.foo(mutableListOf(null))
    val k10: List<Any?> = j.bar()
}