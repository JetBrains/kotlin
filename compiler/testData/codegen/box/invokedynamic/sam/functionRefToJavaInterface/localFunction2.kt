// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 1 java/lang/invoke/LambdaMetafactory

// FILE: localFunction2.kt
fun box(): String {
    val t = "O"
    fun ok() = t + "K"
    return Sam(::ok).get()
}

// FILE: Sam.java
public interface Sam {
    String get();
}
