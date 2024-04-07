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
public class Java2 extends Java1 { }

// FILE: Java3.java
public class Java3 extends Java1<Double>  { }

// FILE: Java4.java
public class Java4<T> extends Java1<Number> {
    public void foo(T t) { }
}

// FILE: Java6.java
public class Java6 extends KotlinClass2 {
    @Override
    public void foo(Number t) {}
    @Override
    public Number bar() {
        return null;
    }
}

// FILE: 1.kt
class A : Java2()

class B : Java3()

class C : Java4<Any>()

class D : Java2() {
    override fun foo(t: Number?) { }
}

class E : Java3() {
    override fun foo(t: Double?) { }
}

class F : Java4<Any>() {
    override fun foo(t: Any?) { }
}

fun test(a: A, b: B, c : C, d: D, e: E, f: F) {
    a.foo(1)
    a.foo(1.1)
    a.foo(null)
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
}