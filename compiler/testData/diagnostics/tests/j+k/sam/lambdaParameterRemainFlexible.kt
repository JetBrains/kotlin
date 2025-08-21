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
        x.length // Should not be unsafe call
    }
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaType, lambdaLiteral, nullableType */
