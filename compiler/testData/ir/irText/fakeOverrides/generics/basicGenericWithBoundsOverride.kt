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
public interface Java3 {
    <U extends Number> void foo(U a);
    <U extends Number> U bar();
}

// FILE: 1.kt
class A: Java1<Int>()

class B : Java1<Int>() {
    override fun bar(): Int {
        return 1
    }
    override fun foo(t: Int?) {}
}

abstract class C : Java2<Double>

class D : Java2<Double> {
    override fun foo(t: Double) { }
    override fun bar(): Double {
        return 1.1
    }
}

abstract class E : Java3

class F : Java3 {
    override fun <U : Number> foo(a: U) { }
    override fun <U : Number> bar(): U {
        return null!!
    }
}

fun test(a: A, b: B, c: C, d: D, e: E, f: F) {
    a.foo(null)
    a.foo(1)
    a.bar()
    b.foo(null)
    b.foo(1)
    b.bar()
    c.foo(null)
    c.foo(1.1)
    c.bar()
    d.foo(1.1)
    d.bar()
    e.foo(1)
    e.bar<Int>()
    f.foo(2.2)
    f.bar<Double>()
}