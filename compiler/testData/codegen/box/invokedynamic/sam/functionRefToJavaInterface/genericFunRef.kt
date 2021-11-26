// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 1 java/lang/invoke/LambdaMetafactory

// FILE: genericFunRef.kt

fun <T> plusK(x: T) = x.toString() + "K"

fun box() = J(::plusK).apply("O")

// FILE: J.java
public interface J {
    public String apply(String x);
}
