// SKIP_KT_DUMP
// TARGET_BACKEND: JVM

// FILE: Java1.java
public class Java1 {
    public void foo(Object... a) {}
}

// FILE: Java2.java
public interface Java2 {
    public void foo(String... a);
}


// FILE: 1.kt
abstract class A : Java1(), Java2

class B : Java1(), Java2 {
    override fun foo(vararg a: String) { }
}

abstract class C : Java2, KotlinInterface

class D: Java2, KotlinInterface {
    override fun foo(vararg a: Any) { }

    override fun foo(vararg a: String?) { }
}

interface KotlinInterface {
    fun foo(vararg a: Any)
}

fun test(a: A, b: B, c: C, d: D) {
    a.foo(1, 2)
    a.foo("1", "2")
    a.foo(null)

    b.foo(1, 2)
    b.foo("1", "2")
    b.foo(null)

    c.foo(1, 2)
    c.foo("1", "2")
    c.foo(null)

    d.foo(1, 2)
    d.foo(null)
}