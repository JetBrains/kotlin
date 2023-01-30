// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K1: JVM_IR
// IGNORE_BACKEND_K2: JVM_IR
// tried to access field B.f from class TestKt

// FILE: A.java

public class A {
    String f = "OK";
}

// FILE: B.kt

open class B : A() {
    private val f = "FAIL"
}

// FILE: C.java

public class C extends B {}

// FILE: test.kt

fun box(): String {
    return C().f
}
