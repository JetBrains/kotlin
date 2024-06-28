// SKIP_KT_DUMP
// TARGET_BACKEND: JVM

// FILE: Java1.java
public class Java1 {
    public void foo(String... a) {}
}

// FILE: Java2.java
public interface Java2 extends KotlinInterface { }

// FILE: 1.kt

class A : Java1()

class B: Java1() {
    override fun foo(vararg a: String?) { }
}

abstract class C: Java2 //Kotlin ← Java ← Kotlin

class D: Java2 {        //Kotlin ← Java ← Kotlin
    override fun foo(vararg a: Any) {}
}

abstract class E : KotlinInterface2 //Kotlin ← Java ← Kotlin ← Java

class F : KotlinInterface2 {        //Kotlin ← Java ← Kotlin ← Java
    override fun foo(vararg a: Any) { }
}

interface KotlinInterface {
    fun foo(vararg a: Any)
}

interface KotlinInterface2 : Java2


fun test(a: A, b: B, c: C, d: D, e: E, f: F) {
    a.foo("","1")
    a.foo(null)

    b.foo("","1")
    b.foo(null)

    c.foo("","1")
    d.foo("","1")
    e.foo("","1")
    f.foo("","1")
}