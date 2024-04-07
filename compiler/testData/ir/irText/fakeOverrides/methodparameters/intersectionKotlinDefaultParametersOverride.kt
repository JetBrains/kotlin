// SKIP_KT_DUMP
// FIR_IDENTICAL
// TARGET_BACKEND: JVM

// FILE: Java1.java
public interface Java1 {
    void foo(Integer a, Object b, Object c);
}

// FILE: Java2.java
public interface Java2 extends KotlinInterface { }

// FILE: 1.kt
open class A {
    open fun foo(a: Int = 0, b: Any? = "string", c: Nothing? = null) { }
}

abstract class B : A(), Java1   //Kotlin ← Java, Kotlin2

class C: A(), Java1 {   //Kotlin ← Java, Kotlin2 with explicit override
    override fun foo(a: Int?, b: Any?, c: Any?) { }
}

class D : A(), Java2 {  //Kotlin ← Java, Kotlin2 ← Kotlin3
    override fun foo(a: Int, b: Any?, c: Nothing?) { }
}

interface KotlinInterface {
    fun foo(a: Int = 1, b: Any? = "string2", c: Nothing? = null) { }
}


fun test(b: B, c: C, d: D) {
    b.foo()
    b.foo(1, null, null)
    b.foo(1, "", "")
    c.foo()
    c.foo(null, "", null)
    c.foo(1, "", null)
    d.foo()
    d.foo(1, null, null)
}