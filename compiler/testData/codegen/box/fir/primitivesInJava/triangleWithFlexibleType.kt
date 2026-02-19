// TARGET_BACKEND: JVM_IR
// ISSUE: KT-62554
// FILE: A.java

public class A {
    public String foo(Integer x) {
        return "FAIL";
    }
}

// FILE: main.kt

interface B {
    fun foo(x: Int) = "OK"
}

class C : A(), B

fun box(): String {
    return C().foo(42)
}
