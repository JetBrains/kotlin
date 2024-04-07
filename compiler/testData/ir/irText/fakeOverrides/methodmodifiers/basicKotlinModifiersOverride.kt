// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// SEPARATE_SIGNATURE_DUMP_FOR_K2
// ^ Difference in external

// FILE: Java1.java

public class Java1 extends A { }

// FILE: 1.kt

open class A {
    inline fun foo() {}
    open external fun foo2()
    open suspend fun foo3() {}
    inline fun <reified T> foo4(t: T){}
}

class B : Java1()   //Kotlin ← Java ← Kotlin

class C : Java1() { //Kotlin ← Java ← Kotlin with explicit override
    override suspend fun foo3() { }
    override fun foo2() {}
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