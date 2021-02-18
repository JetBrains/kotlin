// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY
// FILE: extensionLambda2.kt
fun samExtLambda(ext: String.(String) -> String) = Sam(ext)

fun box(): String {
    val oChar = 'O'
    return samExtLambda { oChar.toString() + this + it }.get("", "K")
}

// FILE: Sam.java
public interface Sam {
    String get(String s1, String s2);
}