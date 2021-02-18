// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY
// FILE: simple.kt
val lambda = { "OK" }

fun box() = Sam(lambda).get()

// FILE: Sam.java
public interface Sam {
    String get();
}