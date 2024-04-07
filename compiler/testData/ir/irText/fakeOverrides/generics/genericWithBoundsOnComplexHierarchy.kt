// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// SEPARATE_SIGNATURE_DUMP_FOR_K2
// ^ ISSUE: KT-66067, KT-59138

// FILE: Java1.java
public class Java1<T extends Number> {
    public void foo(T t) { }
    public T bar() {
        return null;
    }
}

// FILE: Java2.java
public class Java2 extends KotlinClass {
    public void foo(Object t) { }
}

// FILE: Java3.java
interface Java3  {
    void foo(Object t) ;
}

// FILE: Java4.java
public class Java4<T> extends Java1<Number> {
    public void foo(T t) { }
}

// FILE: Java5.java
interface Java5 extends KotlinInterface { }

// FILE: 1.kt
class A : Java2()   // Kotlin ← Java ← Kotlin ← Java

abstract class B : KotlinClass<Int>(), Java3    // Kotlin ← Java, Kotlin2 ← Java2

class C : KotlinClass<Int>(), Java3 {
    override fun foo(t: Any?) { }
}

abstract class D : Java4<Any>(), Java3   // Kotlin ← Java1, Java2 ← Java3

class E : Java4<Any>(), Java3 {
    override fun foo(t: Any?) { }
}

abstract class F : Java1<Int>(), Java5  // Kotlin ← Java1, Java2 ← Kotlin2

class G : Java1<Int>(), Java5 {
    override fun foo(t: Any) { }
}

class H : Java2(), KotlinInterface<Any> // Kotlin ← Java, Kotlin2 ← Kotlin3

class I: Java2(), KotlinInterface<Any> {
    override fun foo(t: Any) { }
}

open class KotlinClass<T> : Java1<T>() where T: Number, T: Comparable<T>

interface KotlinInterface<T: Any>{
    fun foo(t: T)
    fun bar(): T?
}

fun test(a: A, b: B, c: C, d: D, e: E, f: F, h: H, i: I) {
    a.foo(1)
    a.foo(1.1)
    a.foo(null)
    a.foo("")
    a.bar()
    b.foo(1.2)
    b.foo(null)
    b.bar()
    c.foo(null)
    c.foo("")
    c.foo(1)
    c.bar()
    d.foo(null)
    d.foo(1)
    d.bar()
    e.foo(1.1)
    e.foo(null)
    e.bar()
    f.foo(2)
    f.foo("")
    f.bar()
    h.foo(2)
    h.foo("")
    h.bar()
    i.foo(2)
    i.foo("")
    i.bar()
}