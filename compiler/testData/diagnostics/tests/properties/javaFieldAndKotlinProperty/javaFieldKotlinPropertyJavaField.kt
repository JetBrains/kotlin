// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -ProperFieldAccessGenerationForFieldAccessShadowedByKotlinProperty
// ISSUE: KT-56386

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
