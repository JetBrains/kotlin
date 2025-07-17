// RUN_PIPELINE_TILL: FRONTEND
// FILE: test/A.kt
package test

interface <!CLASSIFIER_REDECLARATION!>A<!>

// FILE: test/A.java
package test;

public interface A {
}

/* GENERATED_FIR_TAGS: interfaceDeclaration */
