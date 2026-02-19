// TARGET_BACKEND: JVM_IR
// JVM_TARGET: 1.8
// ISSUE: KT-59380

// FILE: A.java

public interface A {
    static String foo() {
        return "FAIL";
    }
}

// FILE: box.kt

class B : A {
    fun foo() = "OK"
}

fun box() = B().foo()
