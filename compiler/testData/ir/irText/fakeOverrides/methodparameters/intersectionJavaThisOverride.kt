// SKIP_KT_DUMP
// FIR_IDENTICAL
// TARGET_BACKEND: JVM

// FILE: Java1.java
public class Java1 {
    public void foo(Java1 this){ }
}

// FILE: Java2.java
public interface Java2 {
    public void foo();
}

// FILE: 1.kt

class A : Java1(), Java2

class B : Java1(), Java2 {
    override fun foo() { }
}

class C : KotlinInterface, Java1() {
    override fun foo() { }

}

interface KotlinInterface {
    fun foo()
}

fun test(a: A, b: B, c: C) {
    a.foo()
    b.foo()
    c.foo()
}