// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY
// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 1 java/lang/invoke/LambdaMetafactory

// FILE: highOrderFunRef.kt

fun applyO(fn: (String) -> String) = fn("O")

fun box() = J(::applyO).invoke { it + "K" }

// FILE: J.java
public interface J {
    String invoke(kotlin.jvm.functions.Function1<String, String> fn);
}
