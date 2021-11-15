// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY
// WITH_STDLIB
// FULL_JDK

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 1 java/lang/invoke/LambdaMetafactory

// FILE: inlineFunWithReifiedTypeParameterRef.kt

inline fun <reified T> oPlus(x: T) = "O" + T::class.java.simpleName

class K

fun box() = J(::oPlus).apply(K())

// FILE: J.java
public interface J {
    public String apply(K x);
}
