// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY
// FILE: simple.kt
fun ok() = "OK"

fun box() = Sam(::ok).get()

// FILE: Sam.java
public interface Sam {
    String get();
}
