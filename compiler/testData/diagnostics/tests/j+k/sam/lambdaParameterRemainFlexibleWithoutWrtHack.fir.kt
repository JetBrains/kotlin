// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-67999

// FILE: J.java
public interface J<X> {
    void foo(X x);
}

// FILE: main.kt

fun main() {
    J<String?> { x ->
        x<!UNSAFE_CALL!>.<!>length // Should not be unsafe call
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, javaType, lambdaLiteral, nullableType */
