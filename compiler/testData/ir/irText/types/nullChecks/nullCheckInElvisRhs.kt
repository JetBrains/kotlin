// FIR_IDENTICAL
// TARGET_BACKEND: JVM

// MODULE: lib
// FILE: RightElvisOperand.java

class RightElvisOperand {
    static String foo() {
        return null;
    }
}

// MODULE: main(lib)
// FILE: nullCheckInElvisRhs.kt

fun baz(): String? = null

fun test(): String = baz() ?: RightElvisOperand.foo()