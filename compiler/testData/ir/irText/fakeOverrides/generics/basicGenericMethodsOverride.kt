// SKIP_KT_DUMP
// FIR_IDENTICAL
// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB

// FILE: Java1.java
public class Java1 {
    public <T> void foo(T a) { }
    public <T> T bar() {
        return null;
    }
}

// FILE: 1.kt

class A : Java1()

class B : Java1() {
    override fun <T : Any> foo(a: T?) { }
    override fun <T : Any> bar(): T? {
        return null!!
    }
}

fun test(a: A, b: B) {
    val k: Int = a.bar()
    val k2: Int? = a.bar()
    val k3: Any = a.bar()
    val k4: Nothing = a.bar()
    a.foo(1)
    a.foo(null)
    a.foo<Int?>(null)
    a.foo(listOf(null))

    val k5: Int? = b.bar<Int>()
    val k7: Any? = b.bar<Any>()
    val k8: Nothing? = b.bar<Nothing>()
    b.foo(1)
    b.foo(null)
    b.foo<Int>(null)
    b.foo(listOf(null))
}