// SKIP_KT_DUMP
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
public interface Java2 extends KotlinInterface { }

// FILE: Java3.java
public class Java3 extends Java1   {
    @Override
    public <T> void foo(T a) { }
    @Override
    public <T> T bar() {
        return null;
    }
}

// FILE: Java4.java
public class Java4 extends KotlinClass { }

// FILE: 1.kt
class A : Java1(), Java2    //Kotlin ← Java1, Java2 ← Kotlin2

class B : Java1(), Java2 {
    override fun <T : Any?> bar(): T {
        return null!!
    }
    override fun <T : Any?> foo(a: T) { }
}

abstract class C: Java2, KotlinInterface2   //Kotlin ← Java, Kotlin2 ← Kotlin3

abstract class D : Java2, KotlinInterface2 {
    override fun <T : Number> foo(a: T) { }
    override fun <T> bar(): T {
        return null!!
    }
    override fun <T> foo(a: T) { }
}

class E : D() {
    override fun <T : Number> bar(): T {
        return null!!
    }
}

class F : KotlinClass(), Java2  //Kotlin ← Java, Kotlin2 ← Java2, Kotlin3

class G : KotlinClass(), Java2 {
    override fun <T : Any?> bar(): T {
        return null!!
    }
}

class H : Java3(), Java2    //Kotlin ← Java1, Java2 ← Java3, Kotlin2

class I : Java3(), Java2 {
    override fun <T : Any?> foo(a: T) { }
}

class J : Java4() //Kotlin ← Java ← Kotlin ← Java

class L : Java4() {
    override fun <T : Any?> foo(a: T) { }
}

interface KotlinInterface {
    fun <T> foo(a: T)
    fun <T> bar(): T
}

interface KotlinInterface2 {
    fun <T: Number> foo(a: T)
    fun <T: Number> bar(): T
}

open class KotlinClass : Java1()

fun test(a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, l:L) {
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

    val k11: Int? = e.bar<Int?>()
    val k12: Any = e.bar<Any>()
    e.foo(1)
    e.foo(null)
    e.foo<Int?>(null)
    e.foo(listOf(null))

    val k13: Int? = f.bar<Int?>()
    val k14: Any = f.bar<Any>()
    f.foo(1)
    f.foo(null)
    f.foo<Int?>(null)
    f.foo(listOf(null))

    val k15: Int? = g.bar<Int?>()
    val k16: Any = g.bar<Any>()
    g.foo(1)
    g.foo(null)
    g.foo<Int?>(null)
    g.foo(listOf(null))

    val k17: Int? = h.bar<Int?>()
    val k18: Any = h.bar<Any>()
    h.foo(1)
    h.foo(null)
    h.foo<Int?>(null)
    h.foo(listOf(null))

    val k19: Int? = i.bar<Int?>()
    val k20: Any = i.bar<Any>()
    i.foo(1)
    i.foo(null)
    i.foo<Int?>(null)
    i.foo(listOf(null))

    val k21: Int? = j.bar<Int?>()
    val k22: Any = j.bar<Any>()
    j.foo(1)
    j.foo(null)
    j.foo<Int?>(null)
    j.foo(listOf(null))

    l.foo(1)
    l.bar<Int>()
}