// RUN_PIPELINE_TILL: BACKEND
// FIR_DUMP
// LANGUAGE: -DontMakeExplicitJavaTypeArgumentsFlexible
// ISSUE: KT-67999

// FILE: J.java
public interface J<X> {
    void foo(X x);
}

// FILE: main.kt

fun main() {
    J<String?> { x ->
        <!NULLABILITY_MISMATCH_BASED_ON_EXPLICIT_TYPE_ARGUMENTS_FOR_JAVA("String?; String;  This will become an error in a future release. See https://youtrack.jetbrains.com/issue/KTLC-284.")!>x<!>.length // Should not be unsafe call
    }
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaType, lambdaLiteral, nullableType */
