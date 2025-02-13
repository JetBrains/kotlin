// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// LANGUAGE: +JavaTypeParameterDefaultRepresentationWithDNN -DontMakeExplicitJavaTypeArgumentsFlexible
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
