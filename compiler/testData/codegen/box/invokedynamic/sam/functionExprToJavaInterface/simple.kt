// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY
// LAMBDAS: INDY

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 2 java/lang/invoke/LambdaMetafactory

// FILE: simple.kt
val lambda = { "OK" }

fun box() = Sam(lambda).get()

// FILE: Sam.java
public interface Sam {
    String get();
}
