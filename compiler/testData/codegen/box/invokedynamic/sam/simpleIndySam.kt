// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY
// FILE: simpleIndySam.kt
var test = "Failed"

fun box(): String {
    J.run { test = "OK" }
    return test
}

// FILE: J.java
public class J {
    public static void run(Runnable r) {
        r.run();
    }
}