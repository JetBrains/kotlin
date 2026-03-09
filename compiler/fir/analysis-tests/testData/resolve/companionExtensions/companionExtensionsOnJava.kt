// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +CompanionBlocksAndExtensions
// FILE: J.java
public class J {
    public static void foo() {}
    public static String field = "";
}

// FILE: test.kt
companion fun J.bar() {
    foo()
    field
}

/* GENERATED_FIR_TAGS: flexibleType, funWithExtensionReceiver, functionDeclaration, javaFunction, javaProperty, javaType */
