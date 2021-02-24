// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY
// FILE: localFunction2.kt
fun box(): String {
    val t = "O"
    fun ok() = t + "K"
    return Sam(::ok).get()
}

// FILE: Sam.java
public interface Sam {
    String get();
}
