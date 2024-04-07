// SKIP_KT_DUMP
// TARGET_BACKEND: JVM

// FILE: Java1.java
public interface Java1 {
    public void invoke();
    public Java1 plus(Integer i);
    public Integer get(Integer i);
}

// FILE: Java2.java
public interface Java2 {
    public void invoke();
    public Java2 plus(Object i);
    public Object get(Object i);
}

// FILE: 1.kt
abstract class A : Java1, Java2 //Kotlin ← Java1, Java2

class B : Java1, Java2 {    //Kotlin ← Java1, Java2 with explicit override
    override fun invoke() { }

    override fun plus(i: Any?): Java2 {
        return null!!
    }
    override fun get(i: Any?): Any {
        return 2
    }
    override fun plus(i: Int?): Java1 {
        return null!!
    }
    override fun get(i: Int?): Int {
        return 3
    }
}

abstract class C : KotlinInterface, Java1  //Kotlin ← Java, Kotlin2

class D: Java1, KotlinInterface {   //Kotlin ← Java, Kotlin2 with explicit override
    override fun invoke() { }
    override fun plus(i: Int?): Java1 {
        return null!!
    }
    override fun get(i: Int?): Int {
        return 4
    }
}

interface KotlinInterface {
    operator fun invoke()
    operator fun get(i: Int?): Int
}

fun test(a: A, b: B, c: C, d: D) {
    val k: Unit = a()
    val k1: Java1 = a + 1
    val k2: Int? = a[1]
    val k3: Unit = b()
    val k4: Java1 = b + 1
    val k5: Java1 = b + null
    val k6: Java2 = b + ""
    val k7: Int = b[1]
    val k8: Int = b[null]
    val k9: Any = b[""]
    val k10: Unit = c()
    val k11: Int = c[1]
    val k12: Int = c[null]
    val k13: Java1 = c + 1
    val k14: Unit = d()
    val k15: Int = d[1]
    val k16: Int = d[null]
    val k17: Java1 = d + 1
}