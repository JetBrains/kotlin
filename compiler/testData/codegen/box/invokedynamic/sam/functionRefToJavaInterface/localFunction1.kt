// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 1 java/lang/invoke/LambdaMetafactory

// FILE: localFunction1.kt
fun box(): String {
    fun ok() = "OK"
    return Sam(::ok).get()
}

// FILE: Sam.java
public interface Sam {
    String get();
}
