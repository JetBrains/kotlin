// SKIP_KT_DUMP
// TARGET_BACKEND: JVM

// FILE: Java1.java
public class Java1<T> {
    public void foo(T... a) {};
}

// FILE: Java2.java
public interface Java2<T> {
    public void foo(T... a);
}

// FILE: 1.kt
class A<T> : Java1<T>(), Java2<T>

class B<T> : Java1<T>(), Java2<T> {
    override fun foo(vararg a: T) { }
}

abstract class C<T> : Java2<T>, KotlinInterface<T>

class D<T> : Java2<T>, KotlinInterface<T> {
    override fun foo(vararg a: T) { }
}

interface KotlinInterface<T> {
    fun foo(vararg a: T)
}

class E: Java1<Int>(), Java2<Int?>

fun test(a: A<Int>, b: B<Int?>, c: C<Any>, d: D<Any?>, e: E) {
    a.foo(1, 2)
    a.foo(1, null)
    a.foo(null)

    b.foo(1, 2)
    b.foo(1, null)
    b.foo(null)

    c.foo(1, 2)
    c.foo("1", "2", null)
    c.foo(null)

    d.foo(1, 2)
    c.foo("1", "2", null)
    d.foo(null)

    e.foo(1, 2)
    e.foo(1, null)
    e.foo(null)
}