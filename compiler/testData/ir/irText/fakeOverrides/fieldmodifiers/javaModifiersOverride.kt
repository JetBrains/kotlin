// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// FULL_JDK

// FILE: Java1.java
public class Java1 {
    public transient int a = 10;
    public volatile int b = 11;
    public final int c = 12;
    public final transient int d = 13;
}

// FILE: Java2.java
public class Java2 extends KotlinClass { }

// FILE: Java3.java
public class Java3 extends KotlinClass {
    public int a = 1;
    public int b = 2;
    public int c = 3;
    public int d = 4;
}

// FILE: Java4.java
public interface Java4 {
    public int a = 1;
    public int b = 2;
    public int c = 3;
    public int d = 4;
}

// FILE: 1.kt

class A : Java1()    //Kotlin ← Java

class B : Java2()   //Kotlin ← Java ← Kotlin ← Java

class C : Java3()   //Kotlin ← Java ← Kotlin ← Java with explicit override in java

class D : Java1(), Java4    //Kotlin ← Java1, Java2

abstract class E : Java1(), KotlinInterface //Kotlin ← Java1, Kotlin2

class F(override val a: Int,
        override val b: Int,
        override val c: Int,
        override val d: Int) : Java1(), KotlinInterface //Kotlin ← Java1, Kotlin2 with explicit override

open class KotlinClass : Java1()

interface KotlinInterface {
    val a: Int
    val b: Int
    val c: Int
    val d: Int
}

fun test(a: A, b: B, c: C, d: D, e: E, f: F) {
    a.a
    a.b
    a.c
    a.d
    b.a
    b.b
    b.c
    b.d
    c.a
    c.b
    c.c
    c.d
    d.a
    d.b
    d.c
    d.d
    e.a
    e.b
    e.c
    e.d
    f.a
    f.b
    f.c
    f.d
}