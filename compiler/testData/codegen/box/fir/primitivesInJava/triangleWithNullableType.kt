// TARGET_BACKEND: JVM_IR
// ISSUE: KT-62554
// FILE: A.java

import org.jetbrains.annotations.*;

public class A {
    public String foo(@Nullable Integer x) {
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
