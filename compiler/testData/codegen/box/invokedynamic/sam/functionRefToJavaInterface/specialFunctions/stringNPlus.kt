// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 1 java/lang/invoke/LambdaMetafactory

// FILE: stringNPlus.kt
fun test(x: String?, y: Any?) =
    Sam(String?::plus).get(x, y)

fun box() =
    test("O", "K")

// FILE: Sam.java
public interface Sam {
    String get(String x, Object y);
}
