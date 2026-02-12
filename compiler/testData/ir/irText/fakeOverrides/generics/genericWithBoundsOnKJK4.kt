// SKIP_KT_DUMP
// TARGET_BACKEND: JVM

// FILE: Java4.java
public class Java4 extends KotlinClass2 { }

// FILE: 1.kt
class G : Java4()

class H : Java4() {
    override fun bar(): Number {
        return 1
    }
}

open class KotlinClass2<T> where T : Number, T: Comparable<T> {
    open val a : T? = null
    open fun foo(t: T) { }
    open fun bar(): T? {
        return a
    }
}

fun test(g: G, h: H) {
    g.foo(2)
    g.bar()
    h.foo(2)
    h.bar()
}