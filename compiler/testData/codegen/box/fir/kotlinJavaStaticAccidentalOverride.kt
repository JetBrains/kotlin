// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K1: JVM_IR
// IGNORE_CODEGEN_WITH_IR_FAKE_OVERRIDE_GENERATION
// Both K1 and K2 with IR f/o generator report ACCIDENTAL_OVERRIDE on B.foo (see also KT-60283)
// ISSUE: KT-59380

// FILE: A.java

public class A {
    public static String foo() {
        return "FAIL";
    }
}

// FILE: box.kt

class B : A() {
    fun foo() = "OK"
}

fun box() = B().foo()
