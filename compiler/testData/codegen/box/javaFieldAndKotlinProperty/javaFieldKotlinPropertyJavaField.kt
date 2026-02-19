// TARGET_BACKEND: JVM_IR
// COMMENTED[LANGUAGE: +ProperFieldAccessGenerationForFieldAccessShadowedByKotlinProperty] uncomment when KT-56386 is fixed
// IGNORE_BACKEND_K1: JVM_IR
// IGNORE_BACKEND_K2: JVM_IR
// Reason: KT-56386 is not fixed yet
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
