// RUN_PIPELINE_TILL: CODEGEN

// MODULE: common
// FILE: J.java
public class J {
    public static String foo() { return "OK"; }
}

// FILE: common.kt
expect fun bar(): String

// MODULE: jvm()()(common)
// FILE: jvm.kt
actual fun bar(): String = J.foo()

/* GENERATED_FIR_TAGS: actual, expect, functionDeclaration */
