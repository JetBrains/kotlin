// SKIP_KT_DUMP
// FIR_IDENTICAL
// TARGET_BACKEND: JVM

// FILE: Java1.java

public class Java1 extends A { }

// FILE: Java2.java
public class Java2 { }

// FILE: Java3.java
public interface Java3 { }

// FILE: 1.kt

open class A

fun A.foo(a: Int) {}

var A.a: Int
    get() = 1
    set(value) {}

class B : Java1()   //Kotlin ← Java ← Kotlin with kotlin receiver

fun Java2.foo(a: Int) {}

var Java2.a: String
    get() = "java2"
    set(value) {}

class C : Java2()   //Kotlin ← Java with java receiver

fun Java3.foo(a: Any) {}

var Java3.a: String
    get() = "java3"
    set(value) {}

class D : Java2(), Java3 //Kotlin ← Java1, Java2 with java receivers

class E : A(), Java3    //Kotlin ← Java, Kotlin2

fun test(b: B, c: C, d: D, e: E){
    b.a = 10
    b.foo(1)
    c.a = "3"
    c.foo(1)
    d.foo("")
    e.foo("")
}