// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +DontMakeExplicitJavaTypeArgumentsFlexible
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
        x<!UNSAFE_CALL!>.<!>length // Should not be unsafe call
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, javaFunction, javaType, lambdaLiteral, nullableType, samConversion */
