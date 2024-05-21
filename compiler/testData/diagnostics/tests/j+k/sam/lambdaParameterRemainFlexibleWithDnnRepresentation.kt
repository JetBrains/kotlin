// FIR_IDENTICAL
// LANGUAGE: +JavaTypeParameterDefaultRepresentationWithDNN
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
