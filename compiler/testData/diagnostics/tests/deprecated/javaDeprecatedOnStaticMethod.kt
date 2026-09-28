// RUN_PIPELINE_TILL: CODEGEN
// FILE: JavaClass.java
public class JavaClass {
    /**
     * @deprecated Deprecation message
     */
    public static void foo() {}
}

// FILE: main.kt
fun main() {
    JavaClass.<!DEPRECATION!>foo<!>()
}

/* GENERATED_FIR_TAGS: functionDeclaration, javaFunction */
