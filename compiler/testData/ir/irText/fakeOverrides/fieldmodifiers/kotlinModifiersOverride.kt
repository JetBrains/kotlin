// SKIP_KT_DUMP
// TARGET_BACKEND: JVM

// FILE: Java1.java
public class Java1 extends A {  }

// FILE: Java2.java
public interface Java2 {
    Number b = 5;
    Integer a = 1;
}

// FILE: Java3.java
public interface Java3 extends KotlinInterface { }

// FILE: 1.kt
open class A {
    companion object {
        const val a = 0;
    }

    lateinit var b: Number
}

class B : Java1()   //Kotlin ← Java ← Kotlin

class C : A(), Java2    //Kotlin ← Java, Kotlin2

abstract class D : A(), Java3   //Kotlin ← Java, Kotlin2 ← Kotlin3

interface KotlinInterface {
    var a: Int
    var b: Number
}

fun test(b: B, c: C, d: D){
    b.b = 3
    b.b
    c.b = 4
    c.b
    d.a = 2
    d.b = 7
    d.a
    d.b
}