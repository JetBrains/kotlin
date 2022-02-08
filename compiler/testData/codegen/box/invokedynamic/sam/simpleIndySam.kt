// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 1 java/lang/invoke/LambdaMetafactory

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