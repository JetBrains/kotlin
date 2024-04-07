// SKIP_KT_DUMP
// TARGET_BACKEND: JVM

// FILE: Java1.java
public class Java1<T extends Number> {
    public void foo(T t) { }
    public T bar() {
        return null;
    }
}

// FILE: Java2.java
public interface Java2<T extends Number&Comparable>  {
    public void foo(T t);
    public T bar();
}

// FILE: Java3.java
public interface Java3<T>  {
    public void foo(T t);
    public T bar();
}

// FILE: 1.kt
class A: Java1<Int>(), Java2<Int>   //Kotlin ← Java1, Java2

class B : Java1<Int>(), Java2<Int> {
    override fun bar(): Int {
        return 1
    }
}

class C<T>: Java1<T>(), Java2<T> where T: Number, T: Comparable<T>

class D<T>: Java1<T>(), Java2<T> where T: Number, T: Comparable<T> {
    override fun foo(t: T) { }
}

class E : Java1<Number>(), Java3<Number>

class F : Java1<Number>(), Java3<Number> {
    override fun bar(): Number {
        return 2
    }
}

class G : Java1<Int>(), KotlinInterface<Int>    // Kotlin ← Java, Kotlin2

class H<T: Number?>: Java1<T>(), KotlinInterface<T>

class I : Java1<Int>(), KotlinInterface<Int> {
    override fun bar(): Int {
        return 3
    }
}

interface KotlinInterface<T>{
    fun foo(t: T)
    fun bar(): T?
}

fun test(a: A, b: B, c: C<Int>, d: D<Float>, e: E, f: F, g: G, h: H<Int?>, i: I) {
    a.foo(null)
    a.foo(1)
    a.bar()
    b.foo(null)
    b.foo(1)
    b.bar()
    c.foo(null)
    c.foo(1)
    c.bar()
    d.foo(1.1F)
    d.bar()
    e.foo(1)
    e.bar()
    f.foo(2.2)
    f.bar()
    g.foo(2)
    g.bar()
    h.foo(null)
    h.bar()
    i.foo(null)
    i.bar()
}