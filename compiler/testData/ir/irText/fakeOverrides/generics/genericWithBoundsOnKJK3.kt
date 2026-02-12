// SKIP_KT_DUMP
// TARGET_BACKEND: JVM

// FILE: Java3.java
public class Java3<T> extends KotlinClass {
    public void foo(T t) { }
    public Number bar() {
        return null;
    }
}

// FILE: 1.kt
class E : Java3<String>()

class F : Java3<String>() {
    override fun foo(t: Number) { }
}

open class KotlinClass<T: Number> {
    open val a : T? = null
    open fun foo(t: T) { }
    open fun bar(): T? {
        return a
    }
}

fun test(e: E, f: F) {
    e.foo(1)
    e.foo("")
    e.bar()
    f.foo(2)
    f.foo("")
    f.bar()
}