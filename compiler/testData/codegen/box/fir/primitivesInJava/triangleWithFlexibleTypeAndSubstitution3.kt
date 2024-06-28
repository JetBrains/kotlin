// TARGET_BACKEND: JVM_IR
// ISSUE: KT-62554
// FILE: A.java

public class A<T> {
    public String foo(T x) {
        return "FAIL";
    }
}

// FILE: main.kt

interface B {
    fun foo(x: Int) = "OK"
}

open class C : A<Int>()

class D : C(), B

fun box(): String {
    return D().foo(42)
}
