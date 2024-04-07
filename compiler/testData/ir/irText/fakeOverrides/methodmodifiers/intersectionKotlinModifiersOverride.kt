// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// SEPARATE_SIGNATURE_DUMP_FOR_K2
// ^ Difference in external

// FILE: Java1.java
public interface Java1 {
    public void foo();
    public void foo2();
    public void foo4();
}

// FILE: 1.kt

open class A {
    inline fun foo() {}
    open external fun foo2()
    open suspend fun foo3() {}
    inline fun <reified T> foo4(t: T){}
}

abstract class B : A(), Java1   //Kotlin ← Java, Kotlin2

class C : A(), Java1 {
    override fun foo2() { }
    override fun foo4() { }
    override suspend fun foo3() {}
}

suspend fun test(b: B, c: C) {
    b.foo()
    b.foo2()
    b.foo3()
    b.foo4(1)
    c.foo()
    c.foo2()
    c.foo3()
    c.foo4("")
}