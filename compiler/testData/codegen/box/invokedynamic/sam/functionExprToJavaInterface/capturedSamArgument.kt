// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY
// FILE: capturedSamArgument.kt
fun box(): String {
    var lambda = { "OK" }
    val sam = Sam(lambda)
    lambda = { "Failed" }
    return sam.get()
}

// FILE: Sam.java
public interface Sam {
    String get();
}