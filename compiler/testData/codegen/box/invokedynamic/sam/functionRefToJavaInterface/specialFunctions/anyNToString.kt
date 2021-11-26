// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 1 java/lang/invoke/LambdaMetafactory

// FILE: anyNToString.kt
fun box() =
    Sam(Any?::toString).get("OK")

// FILE: Sam.java
public interface Sam {
    String get(String s);
}
