// RUN_PIPELINE_TILL: FRONTEND
// FILE: test/A.kt
package test

object <!CLASSIFIER_REDECLARATION!>A<!> {
    class <!CLASSIFIER_REDECLARATION!>Nested<!>
}

// FILE: test/A.java
package test;

public class A {
    public static class Nested {
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, nestedClass, objectDeclaration */
