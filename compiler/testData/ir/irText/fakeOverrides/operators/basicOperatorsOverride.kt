// SKIP_KT_DUMP
// TARGET_BACKEND: JVM

// FILE: Java1.java

public class Java1 {
    public void invoke(){};
    public Java1 plus(Integer i){ return this;};
    public Integer get(Integer i){ return 1; }
}

// FILE: 1.kt

class A : Java1()

class B : Java1() {
    override fun invoke() {}
    override fun get(i: Int?): Int {
        return 2
    }
    override fun plus(i: Int?): Java1 {
        return Java1()
    }
}

class C : Java1() {
    override operator fun invoke() {}
    override operator fun get(i: Int?): Int {
        return 2
    }
    override operator fun plus(i: Int?): Java1 {
        return Java1()
    }
}
fun test() {
    val a = A()
    val k: Unit = a()
    val k1: Java1 = a + 1
    val k2: Int = a[1]
    val b = B()
    val k3: Unit = b()
    val k4: Java1 = b + 1
    val k5: Int = b[1]
    val c = C()
    val k6: Unit = c()
    val k7: Java1 = c + 1
    val k8: Int = c[1]
}