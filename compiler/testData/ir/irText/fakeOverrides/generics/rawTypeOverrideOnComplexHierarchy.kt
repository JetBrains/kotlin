// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// WITH_STDLIB

// FILE: Java1.java
public class Java1 extends KotlinClass { }

// FILE: Java2.java
import java.util.*;

public interface Java2 {
    List a = new ArrayList();
    void foo(List a);
    List bar();
}

// FILE: Java3.java
public interface Java3 extends Java2 { }

// FILE: Java4.java
public interface Java4 extends KotlinInterface2 { }

// FILE: 1.kt
class A : Java1(), Java2    //Kotlin ← Java1, Java2 ← Kotlin2

class B : Java1(), Java2 {
    override fun bar(): List<*> {
        return mutableListOf(2)
    }
    override var a: List<*>
        get() = mutableListOf(2)
        set(value) {}
    override fun foo(a: List<*>) { }
}

class C : Java1(), KotlinInterface  // Kotlin ← Java, Kotlin2 ← Kotlin3

class D : Java1(), KotlinInterface {
    override var a: List<*>
        get() = mutableListOf(2)
        set(value) {}
    override fun foo(a: List<*>) { }
}

class E : Java1(), KotlinInterface2 // Kotlin ← Java, Kotlin2 ← Java2, Kotlin3

class F : Java1(), KotlinInterface2 {
    override fun bar(): List<Any?> {
        return super.bar()
    }
}

class G : Java1(), Java3    // Kotlin ← Java1, Java2 ← Java3, Kotlin2

class H : Java1(), Java3 {
    override fun foo(a: List<*>) { }
}

abstract class I : Java4    //Kotlin ← Java ← Kotlin ← Java

class J : Java4 {
    override fun foo(a: MutableList<Any?>?) { }

    override fun bar(): MutableList<Any?> {
        return mutableListOf(3)
    }
}

fun test(a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J) {
    a.bar()
    a.foo(mutableListOf(null))
    val k: List<Any?> = a.bar()
    b.foo(mutableListOf(null))
    val k2: List<Any?> = b.bar()
    c.foo(listOf(null))
    val k3: List<Any?> = c.bar()
    d.foo(mutableListOf(null))
    val k4: List<Any?> = d.bar()
    e.foo(mutableListOf(null))
    val k5: List<Any?> = e.bar()
    f.foo(mutableListOf(null))
    val k6: List<Any?> = f.bar()
    g.foo(mutableListOf(null))
    val k7: List<Any?> = g.bar()
    h.foo(mutableListOf(null))
    val k8: List<Any?> = h.bar()
    i.foo(mutableListOf(null))
    val k9: List<Any?> = i.bar()
    j.foo(mutableListOf(null))
    val k10: List<Any?> = j.bar()
}

open class KotlinClass {
    open var a: List<*> = mutableListOf("1")
    open fun foo(a: List<*>) {
    }
    open fun bar(): List<*> {
        return mutableListOf("1")
    }
}

interface KotlinInterface {
    var a: List<Any?>
    fun foo(a: List<Any?>)
    fun bar(): List<Any?>
}

interface KotlinInterface2 : Java2