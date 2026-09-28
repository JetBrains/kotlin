// TARGET_BACKEND: JVM
// LANGUAGE: +CompanionBlocks
// FILE: J.java
public class J {
    public static String invoke(String s) {
        return s;
    }
}

// FILE: box.kt
fun box(): String {
    return J("OK")
}
