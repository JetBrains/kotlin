// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// FILE: JavaClass.java
public class JavaClass {
    void from(String s) {}
    void from(String... s) {}
}

// FILE: kotlin.kt
fun main() {
    JavaClass().from()
    JavaClass().from("")
    JavaClass().from("", "")
}

/* GENERATED_FIR_TAGS: functionDeclaration, javaFunction, javaType, stringLiteral */
