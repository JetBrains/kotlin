// SKIP_KT_DUMP
// TARGET_BACKEND: JVM

// FILE: Java1.java
public class Java1<T> {
    public void foo(T... a) {}
}

// FILE: 1.kt
class A : Java1<Int>()

class B: Java1<Any>() {
    override fun foo(vararg a: Any) { }
}

fun test(a: A, b: B) {
    a.foo(1,2)
    a.foo(null)
    b.foo("", 1)
}