// SKIP_KT_DUMP
// TARGET_BACKEND: JVM

// Bug: mismatch on some parameters' types flexibility.
// The duplicated `foo` member mismatch should automatically get fixed when the flexibility bug is fixed
// KOTLIN_REFLECT_DUMP_MISMATCH

// FILE: Java2.java
public class Java2 extends KotlinClass<Integer> { }

// FILE: 1.kt
class C : Java2()

class D : Java2() {
    override fun bar(): Int? {
        return 6
    }
}

open class KotlinClass<T: Number> {
    open val a : T? = null
    open fun foo(t: T) { }
    open fun bar(): T? {
        return a
    }
}

fun test(c: C, d: D) {
    c.foo(null)
    c.foo(1)
    c.bar()
    d.foo(null)
    d.foo(1)
    d.bar()
}