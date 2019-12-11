
// FILE: A.java

import kotlin.jvm.functions.FunctionN;

public class A {
    public void foo(FunctionN<?> w) { }
}

// FILE: main.kt

class B : A()

fun foo() {
    object : A() {}
}
