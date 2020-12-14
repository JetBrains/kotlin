// WITH_SIGNATURES
// FILE: t.kt

fun <T> genericSam(f: () -> T): Sam<T> = J.sam({ f() })

fun <T> genericSamGet(f: () -> T): T = J.get({ f() })

// FILE: J.java
public class J {
    static <T> T get(Sam<T> s) {
        return s.get();
    }

    static <T> Sam<T> sam(Sam<T> s) {
        return s;
    }
}

// FILE: Sam.java
public interface Sam<T> {
    T get();
}