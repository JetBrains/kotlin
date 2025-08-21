// RUN_PIPELINE_TILL: FRONTEND
// FILE: test/A.kt
package test

object A {
    class Nested
}

// FILE: test/A.java
package test;

public class A {
    public static class Nested {
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, nestedClass, objectDeclaration */
