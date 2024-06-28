// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY
// LAMBDAS: INDY

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 2 java/lang/invoke/LambdaMetafactory

// FILE: genericSam2.kt

fun <FT, FR> test(lambda: (FT) -> FR) = Sam(lambda)

fun box(): String = test<String, String> { "O" + it }.get("K")

// FILE: Sam.java
public interface Sam<T, R> {
    R get(T x);
}
