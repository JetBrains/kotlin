// RUN_PIPELINE_TILL: FRONTEND
// FILE: test/A.kt
package test

object <!CLASSIFIER_REDECLARATION!>A<!>

// FILE: test/A.java
package test;

public class A {
}

/* GENERATED_FIR_TAGS: objectDeclaration */
