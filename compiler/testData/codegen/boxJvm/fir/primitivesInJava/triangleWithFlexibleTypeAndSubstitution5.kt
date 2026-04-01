// TARGET_BACKEND: JVM_IR
// ISSUE: KT-62554
// FILE: A.java

public class A {
    public String foo(Integer x) {
        return "FAIL";
    }
}

// FILE: main.kt

interface B<T> {
    fun foo(x: T) = "OK"
}

interface D : B<Int>

class E : A(), D

fun box(): String {
    return E().foo(42)
}
