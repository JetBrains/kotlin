// SKIP_KT_DUMP
// FIR_IDENTICAL
// TARGET_BACKEND: JVM

// MODULE: separate
// FILE: KotlinInternalSeparate.kt
open class KotlinInternalSeparate {
    @PublishedApi
    internal open val a : Int
        get() = 1
    @PublishedApi
    internal open fun foo() {}
}

// MODULE: main(separate)
// FILE: JavaDefault.java
public interface JavaDefault {
    int a = 2;
    void foo();
}

// FILE: JavaPublic.java
public interface JavaPublic {
    public int a = 2;
    public void foo();
}

// FILE: test.kt
open class InternalVisibility {
    @PublishedApi
    internal open val a: Int = 4
    @PublishedApi
    internal open fun foo() {}
}

class A : JavaDefault, InternalVisibility() {
    override fun foo() {}
    override val a: Int
        get() = 10
}

class B : JavaPublic, InternalVisibility() {
    override fun foo() {}
    override val a: Int
        get() = 10
}

abstract class C : JavaDefault, KotlinInternalSeparate()

class D : JavaDefault, KotlinInternalSeparate() {
    override fun foo() {}
}

abstract class E : JavaPublic, KotlinInternalSeparate()

class F : JavaPublic, KotlinInternalSeparate() {
    override fun foo() {}
}

fun test(a: A, b: B, c: C, d: D, e: E, f: F){
    a.foo()
    a.a
    b.foo()
    b.a
    c.foo()
    d.foo()
    f.foo()
}