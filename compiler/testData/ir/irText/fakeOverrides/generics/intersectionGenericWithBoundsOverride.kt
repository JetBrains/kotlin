// SKIP_KT_DUMP
// TARGET_BACKEND: JVM

// FILE: Java1.java
public class Java1<T extends Number> {
    public void foo(T t) { }
    public T bar() {
        return null;
    }
}
// FILE: 1.kt

class I : Java1<Int>(), KotlinInterface<Int> { // Collapsed foo
    override fun bar(): Int {
        return 3
    }
}

interface KotlinInterface<T>{
    fun foo(t: T)
    fun bar(): T?
}
