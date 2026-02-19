// TARGET_BACKEND: JVM_IR
// ISSUE: KT-62554
// FILE: A.java

public class A<T> {
    public String foo(T x) {
        return "FAIL";
    }
}

// FILE: C.java

public class C extends A<Integer> {}

// FILE: main.kt

interface B {
    fun foo(x: Int) = "OK"
}

class D : C(), B

fun box(): String {
    return D().foo(42)
}
