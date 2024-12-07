// TARGET_BACKEND: JVM

// FILE: I.java
interface I {
    String result = "OK";
}

// FILE: E.java
public enum E implements I {}

// FILE: box.kt
fun box(): String = E.result
