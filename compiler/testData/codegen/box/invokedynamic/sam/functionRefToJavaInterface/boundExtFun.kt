// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY
// FILE: boundExtFun.kt
fun String.k(s: String) = this + s + "K"

fun box() = Sam("O"::k).get("")
// NB simply '::k' is a compilation error

// FILE: Sam.java
public interface Sam {
    String get(String s);
}
