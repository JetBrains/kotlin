// TARGET_BACKEND: JVM_IR
// COMMENTED[LANGUAGE: +ProperFieldAccessGenerationForFieldAccessShadowedByKotlinProperty] uncomment when KT-56386 is fixed
// IGNORE_BACKEND_K1: JVM_IR
// IGNORE_BACKEND_K2: JVM_IR
// Reason: KT-56386 is not fixed yet
// CHECK_BYTECODE_TEXT

// FILE: Y.java
package base;

class Y {
    public String f = "OK";
}

// FILE: A.java
package base;

public class A extends Y {}

// FILE: B.kt
package base

open class B : A() {
    private val f = "FAIL"
}

// FILE: C.java
import base.B;

public class C extends B {}

// FILE: test.kt
fun box(): String {
    return C().f
}

// 1 GETFIELD base/A.f
