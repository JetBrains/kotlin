// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// IGNORE_BACKEND_K1: ANY
// SEPARATE_SIGNATURE_DUMP_FOR_K2
// ^ ISSUE: KT-65679, KT-65388

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
// FILE: Java1.java
public class Java1 extends InternalVisibility { }

// FILE: Java2.java
public class Java2 extends InternalVisibility {
    public int a = 2;
    public void foo(){}
}

// FILE: Java3.java
public class Java3 extends KotlinInternalSeparate { }

// FILE: Java4.java
public class Java4 extends KotlinInternalSeparate {
    public int a = 4;
    public void foo(){}
}

// FILE: test.kt
open class InternalVisibility {
    @PublishedApi
    internal open val a: Int = 4
    @PublishedApi
    internal open fun foo() {}
}

class A : Java1()   //Kotlin ← Java ← Kotlin(internal)

class B : Java1() {
    override fun foo() {}
}

class C : Java2()   //Kotlin ← Java(public) ← Kotlin(internal)

class D : Java2() {
    override fun foo() {}
    override val a: Int
        get() = 10
}

class E : Java3()   //Kotlin ← Java ← Kotlin(internal separate module)

class F : Java4()   //Kotlin ← Java(public) ← Kotlin(internal separate module)

class G : Java4() {
    override fun foo() {}
}

fun test(a: A, b: B, c: C, d: D, e: E, f: F, g: G){
    a.foo()
    a.a
    b.foo()
    b.a
    c.foo()
    c.a
    d.foo()
    d.a
    f.foo()
    f.a
    g.foo()
    g.a
}