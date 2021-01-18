// WITH_SIGNATURES
// FILE: t.kt

fun specializedSam(f: () -> String) = J.g({ f() })

// FILE: J.java
public class J {
    static <T> T g(Sam<T> s) {
        return s.get();
    }
}

// FILE: Sam.java
public interface Sam<T> {
    T get();
}