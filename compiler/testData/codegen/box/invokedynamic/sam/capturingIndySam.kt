// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY
// FILE: capturingIndySam.kt
var test = "Failed"

fun box(): String {
    val ok = "OK"
    J.run { test = ok }
    return test
}

// FILE: J.java
public class J {
    public static void run(Runnable r) {
        r.run();
    }
}