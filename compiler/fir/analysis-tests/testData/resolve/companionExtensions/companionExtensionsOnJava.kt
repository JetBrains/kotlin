// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocksAndExtensions
// FILE: J.java
public class J {
    public static void foo() {}
    public static String field = "";
}

// FILE: test.kt
<!WRONG_MODIFIER_TARGET!>companion<!> fun J.bar() {
    foo()
    field
}

/* GENERATED_FIR_TAGS: flexibleType, funWithExtensionReceiver, functionDeclaration, javaFunction, javaProperty, javaType */
