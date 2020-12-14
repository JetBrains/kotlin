// WITH_SIGNATURES
// FILE: t.kt

fun foo(): String = ""

fun specializedSam(): String = J.g(::foo)

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