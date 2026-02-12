// SKIP_KT_DUMP
// TARGET_BACKEND: JVM

// FILE: Java1.java
public class Java1 extends KotlinClass { }

// FILE: 1.kt
class A : Java1()

class B : Java1() {
    override fun foo(t: Number) { }
}

open class KotlinClass<T: Number> {
    open val a : T? = null
    open fun foo(t: T) { }
    open fun bar(): T? {
        return a
    }
}

fun test(a: A, b: B) {
    a.foo(1)
    a.foo(1.1)
    a.bar()
    b.foo(1.2)
    b.foo(1)
    b.bar()
}