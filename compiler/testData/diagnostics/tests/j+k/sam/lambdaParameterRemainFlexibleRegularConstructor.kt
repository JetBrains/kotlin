// RUN_PIPELINE_TILL: FRONTEND
// FIR_DUMP
// ISSUE: KT-67999

// FILE: J.java
public class J<T> {
    J(F<T> f) {}

    public interface F<E> {
        void foo(E e);
    }
}

// FILE: main.kt
fun main() {
    J<String?> { x ->
        x.length // Should not be unsafe call
    }
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaFunction, javaType, lambdaLiteral, nullableType,
samConversion */
