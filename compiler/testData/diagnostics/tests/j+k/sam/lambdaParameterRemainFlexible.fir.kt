// RUN_PIPELINE_TILL: BACKEND
// FIR_DUMP
// LANGUAGE: -JavaTypeParameterDefaultRepresentationWithDNN -ForbidTypePreservingFlexibilityWriteInferenceHack
// ISSUE: KT-67999

// FILE: J.java
public interface J<X> {
    void foo(X x);
}

// FILE: main.kt

fun main() {
    J<String?> { x ->
        <!RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>x<!>.length // Should not be unsafe call
    }
}
