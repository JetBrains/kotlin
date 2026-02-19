// SKIP_KT_DUMP
// TARGET_BACKEND: JVM

// FILE: Java5.java
public class Java5 extends KotlinClass2<Integer> { }

// FILE: 1.kt
class I : Java5()

class J : Java5() {
    override fun foo(t: Int?) { }
}

open class KotlinClass2<T> where T : Number, T: Comparable<T> {
    open val a : T? = null
    open fun foo(t: T) { }
    open fun bar(): T? {
        return a
    }
}

fun test(i: I, j: J) {
    i.foo(2)
    i.bar()
    j.foo(null)
    j.foo(1)
    j.bar()
}