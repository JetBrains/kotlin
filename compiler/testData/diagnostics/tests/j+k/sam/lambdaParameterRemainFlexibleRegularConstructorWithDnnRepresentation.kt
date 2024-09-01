// FIR_IDENTICAL
// LANGUAGE: +JavaTypeParameterDefaultRepresentationWithDNN
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
