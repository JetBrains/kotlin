// KOTLIN_CONFIGURATION_FLAGS: SAM_CONVERSIONS=CLASS
// WITH_SIGNATURES
// FILE: t.kt

fun <T> genericSam(f: () -> T): T = J.g(f)

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