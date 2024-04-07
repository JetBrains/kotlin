// SKIP_KT_DUMP
// FIR_IDENTICAL
// TARGET_BACKEND: JVM
// WITH_STDLIB

// FILE: Java1.java
public class Java1 {
    public <T> void foo(T a) { }
    public <T> T bar() {
        return null;
    }
}

// FILE: Java2.java
public class Java2 extends Java1 { }

// FILE: Java3.java
public class Java3 extends Java1   {
    @Override
    public <T> void foo(T a) { }
    @Override
    public <T> T bar() {
        return null;
    }
}

// FILE: 1.kt
abstract class A : Java2()    // Kotlin ← Java1 ← Java2

class B : A() {
    override fun <T> bar(): T {
        return null!!
    }
    override fun <T> foo(a: T) { }
}

class C : Java3()   //Kotlin ← Java1(override) ← Java2

class D : Java1() {
    override fun <T : Any?> bar(): T {
        return null!!
    }
    override fun <T> foo(a: T) {
        println(a)
    }
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