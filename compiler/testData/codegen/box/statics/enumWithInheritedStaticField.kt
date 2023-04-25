// TARGET_BACKEND: JVM
// IGNORE_LIGHT_ANALYSIS
// FILE: E.java

interface I {
    String result = "OK";
}

public enum E implements I {}

// FILE: box.kt

fun box(): String = E.result
