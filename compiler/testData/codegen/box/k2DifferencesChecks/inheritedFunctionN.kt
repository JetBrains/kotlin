// ORIGINAL: /compiler/testData/diagnostics/testsWithStdLib/java/inheritedFunctionN.fir.kt
// WITH_STDLIB

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


fun box() = "OK".also { foo() }
