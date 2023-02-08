// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K1: JVM_IR
// CHECK_BYTECODE_TEXT

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

// 1 GETFIELD A.f
