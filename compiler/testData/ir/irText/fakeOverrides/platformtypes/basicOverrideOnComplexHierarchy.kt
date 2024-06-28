// SKIP_KT_DUMP
// TARGET_BACKEND: JVM

// FILE: Java1.java

public class Java1 extends A { }

// FILE: Java2.java

public interface Java2 extends KotlinInterface { }

// FILE: Java3.java

public interface Java3 {
    public Integer a = 0;
    public Integer foo();
    public void bar(Integer o);
}

// FILE: 1.kt
open class A {
    open val a: Int = 1
    open var b: Int = 1
    open fun foo(): Int {
        return 0
    }
    open fun bar(o: Int) {}
}

class B : Java1()  //Kotlin ← Java ← Kotlin

class C : Java1() {     //Kotlin ← Java ← Kotlin with explicit override
    override fun bar(o: Int) {}

    override fun foo(): Int {
        return 2
    }

    override var b: Int
        get() = 2
        set(value) {}

    override val a: Int
        get() = 2
}

interface D : Java2     //Kotlin ← Java ← Kotlin ← Java

class E : Java2 { //Kotlin ← Java ← Kotlin ← Java with explicit override
    override fun foo(): Int {
        return 2
    }

    override fun bar(o: Int) {}
}

interface KotlinInterface : Java3

fun test(a: A, b: B, c: C, d: D, e: E) {
    val k1: Int = a.a
    val k2: Int = a.b
    a.b = 3
    val k3: Int = a.foo()
    val k4: Unit = a.bar(1)
    val k5 = b.a
    val k6 = b.b
    b.b = 3
    val k7: Int = b.foo()
    val k8: Unit = b.bar(1)
    val k9 = c.a
    val k10 = c.b
    c.b = 3
    val k11: Int = c.foo()
    val k12: Unit = c.bar(1)
    val k17: Int = d.foo()
    val k18: Unit = d.bar(1)
    val k19: Int = e.foo()
    val k20: Unit = e.bar(1)
}