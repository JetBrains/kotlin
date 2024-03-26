// SKIP_KT_DUMP
// TARGET_BACKEND: JVM

// FILE: Java1.java
public class Java1<T> {
    public void invoke(){};
    public Java1 plus(T i){ return this;};
    public T get(T i){return null;}
}
// FILE: 1.kt
class A : Java1<Int?>()

class B : Java1<Any>() {
    override fun invoke() {}
    override fun get(i: Any?): Any {
        return 1
    }
    override fun plus(i: Any?): Java1<Any> {
        return Java1<Any>()
    }
}

fun test(a: A, b: B) {
    val a = A()
    val k: Unit = a()
    val k1: Java1<*>? = a + 1
    val k2: Int? = a[1]
    val b = B()
    val k3: Unit = b()
    val k4: Java1<*> = b + 1
    val k5: Any = b[1]
}