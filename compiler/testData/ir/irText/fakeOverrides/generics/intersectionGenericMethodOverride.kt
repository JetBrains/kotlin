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
public interface Java2  {
    public <T> void foo(T a);
    public <T> T bar();
}

// FILE: 1.kt
class A : Java1(), Java2    //Kotlin ← Java1, Java2

class B : Java1(), Java2 {
    override fun <T : Any?> bar(): T {
        return null!!
    }

    override fun <T : Any?> foo(a: T) { }
}

abstract class C : Java1(), KotlinInterface // Kotlin ← Java, Kotlin2

class D : Java1(), KotlinInterface {
    override fun <T : Any?> bar(): T {
        return null!!
    }
    override fun <T : Any?> foo(a: T) { }
}

class E : Java1(), Java2, KotlinInterface   // Kotlin ← Java1, Java2, Kotlin2

class F : Java1(), Java2, KotlinInterface {
    override fun <T : Any?> bar(): T {
        return null!!
    }
}

interface KotlinInterface {
    fun <T> foo(a: T)
    fun <T> bar(): T
}

fun test(a: A, b: B, c: C, d: D, e: E, f: F) {
    val k: Int = a.bar<Int?>()
    val k3: Any = a.bar()
    val k4: Nothing = a.bar()
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

    val k11: Int? = e.bar<Int?>()
    e.foo(1)
    e.foo(null)

    val k12: Any? = f.bar<Any?>()
    f.foo(1)
}