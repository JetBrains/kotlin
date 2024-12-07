// TARGET_BACKEND: JVM_IR
// ISSUE: KT-62554
// FILE: A.java

public class A<T> {
    public T foo(Integer x) {
        return null;
    }
}

// FILE: main.kt

interface B {
    fun foo(x: Int) = "OK"
}

open class C : A<String>()

class D : C(), B

fun box(): String {
    return D().foo(42)
}
