// RUN_PIPELINE_TILL: FRONTEND
// FILE: test/A.kt
package test

enum class A {
    FOO, BAR
}

// FILE: test/A.java
package test;

public enum A {
    FOO, BAR
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry */
