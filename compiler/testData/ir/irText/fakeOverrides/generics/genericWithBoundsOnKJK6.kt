// SKIP_KT_DUMP
// TARGET_BACKEND: JVM

// FILE: Java6.java
public class Java6 extends KotlinClass2 {
    @Override
    public void foo(Number t) {}
    @Override
    public Number bar() {
        return null;
    }
}

// FILE: 1.kt
class K : Java6()

class L : Java6() {
    override fun bar(): Number {
        return 4
    }
}

open class KotlinClass2<T> where T : Number, T: Comparable<T> {
    open val a : T? = null
    open fun foo(t: T) { }
    open fun bar(): T? {
        return a
    }
}

fun test(k: K, l: L) {
    k.foo(1)
    k.bar()
    l.foo(4)
    l.bar()
}