// SKIP_KT_DUMP
// TARGET_BACKEND: JVM

// FILE: Java1.java
public class Java1 extends A { }

// FILE: Java2.java
public class Java2 extends A {
    public void invoke(){};
}

// FILE: Java3.java
public interface Java3 {
    public void invoke();
    public Java3 plus(Integer i);
    public Integer get(Integer i);
}

// FILE: Java4.java
public interface Java4 extends KotlinInterface{ }

// FILE: 1.kt

open class A {
    open operator fun invoke() {}
    open operator fun get(i: Int?): Int {
        return 2
    }
    open operator fun plus(i: Int?): A {
        return A()
    }
}
class B : Java1()   //Kotlin ← Java ← Kotlin

class C : Java2()   //Kotlin ← Java ← Kotlin with explicit override in java

class D: Java1() {  //Kotlin ← Java ← Kotlin with explicit override
    override fun invoke() { }
    override fun get(i: Int?): Int {
        return 3
    }
    override fun plus(i: Int?): A {
        return A()
    }
}

abstract class E : Java4    //Kotlin ← Java ← Kotlin ← Java

class F: Java4 {    //Kotlin ← Java ← Kotlin ← Java with explicit override
    override fun invoke() { }
    override fun plus(i: Int?): Java3 {
        return null!!
    }
    override fun get(i: Int?): Int {
        return 4
    }
}

interface KotlinInterface : Java3

fun test(b: B, c: C, d: D, e: E, f: F) {
    val k: Unit = b()
    val k1: A = b + 1
    val k2: Int = b[1]
    val k3: Unit = c()
    val k4: A = c + 1
    val k5: Int = c[1]
    val k6: Unit = d()
    val k7: A = d + 1
    val k8: Int = d[1]
    val k9: Unit = e()
    val k10: Java3 = e + 1
    val k11: Int = e[1]
    val k12: Unit = f()
    val k13: Java3 = f + 1
    val k14: Int = f[1]
}