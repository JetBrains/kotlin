// SKIP_KT_DUMP
// TARGET_BACKEND: JVM

// FILE: Java1.java
public class Java1 extends KotlinClass { }

// FILE: Java2.java
public class Java2 extends KotlinClass<Integer> { }

// FILE: Java3.java
public class Java3<T> extends KotlinClass {
    public void foo(T t) { }
    public Number bar() {
        return null;
    }
}

// FILE: Java4.java
public class Java4 extends KotlinClass2 { }

// FILE: Java5.java
public class Java5 extends KotlinClass2<Integer> { }

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
class A : Java1()

class B : Java1() {
    override fun foo(t: Number) { }
}

class C : Java2()

class D : Java2() {
    override fun bar(): Int? {
        return 6
    }
}

class E : Java3<String>()

class F : Java3<String>() {
    override fun foo(t: Number) { }
}

class G : Java4()

class H : Java4() {
    override fun bar(): Number {
        return 1
    }
}

class I : Java5()

class J : Java5() {
    override fun foo(t: Int?) { }
}

class K : Java6()

class L : Java6() {
    override fun bar(): Number {
        return 4
    }
}

open class KotlinClass<T: Number> {
    open val a : T? = null
    open fun foo(t: T) { }
    open fun bar(): T? {
        return a
    }
}

open class KotlinClass2<T> where T : Number, T: Comparable<T> {
    open val a : T? = null
    open fun foo(t: T) { }
    open fun bar(): T? {
        return a
    }
}

fun test(a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L) {
    a.foo(1)
    a.foo(1.1)
    a.bar()
    b.foo(1.2)
    b.foo(1)
    b.bar()
    c.foo(null)
    c.foo(1)
    c.bar()
    d.foo(null)
    d.foo(1)
    d.bar()
    e.foo(1)
    e.foo("")
    e.bar()
    f.foo(2)
    f.foo("")
    f.bar()
    g.foo(2)
    g.bar()
    h.foo(2)
    h.bar()
    i.foo(2)
    i.bar()
    j.foo(null)
    j.foo(1)
    j.bar()
    k.foo(1)
    k.bar()
    l.foo(4)
    l.bar()
}