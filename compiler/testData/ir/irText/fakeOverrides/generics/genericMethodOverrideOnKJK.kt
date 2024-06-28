// SKIP_KT_DUMP
// FIR_IDENTICAL
// TARGET_BACKEND: JVM
// WITH_STDLIB

// FILE: Java1.java
public class Java1 implements KotlinInterface {
    @Override
    public <T> void foo(T a) { }
    @Override
    public <T> T bar() {
        return null;
    }
}

// FILE: Java2.java
public interface Java2 extends KotlinInterface { }

// FILE: 1.kt
abstract class A : Java2    // Kotlin ← Java ← Kotlin

class B : A() {
    override fun <T> bar(): T {
        return null!!
    }
    override fun <T> foo(a: T) { }
}

class C : Java1()   //Kotlin ← Java(override) ← Kotlin

class D : Java1() {
    override fun <T : Any?> bar(): T {
        return null!!
    }
    override fun <T> foo(a: T) { }
}

interface KotlinInterface {
    fun <T> foo(a: T)
    fun <T> bar(): T
}

fun test(a: A, b: B, c: C, d: D) {
    val k: Int = a.bar<Int>()
    val k3: Any = a.bar()
    a.foo(1)
    a.foo(null)
    a.foo<Int?>(null)
    a.foo(listOf(null))

    val k5: Int? = b.bar<Int?>()
    val k6: Any = b.bar<Any>()
    b.foo(1)
    b.foo(null)
    b.foo<Int?>(null)
    b.foo(listOf(null))

    val k7: Int? = c.bar<Int?>()
    val k8: Any = c.bar<Any>()
    c.foo(1)
    c.foo(null)
    c.foo<Int?>(null)
    c.foo(listOf(null))

    val k9: Int? = d.bar<Int?>()
    val k10: Any = d.bar<Any>()
    d.foo(1)
    d.foo(null)
    d.foo<Int?>(null)
    d.foo(listOf(null))
}