// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 1 java/lang/invoke/LambdaMetafactory

// FILE: boundExtFun.kt
fun String.k(s: String) = this + s + "K"

fun box() = Sam("O"::k).get("")
// NB simply '::k' is a compilation error

// FILE: Sam.java
public interface Sam {
    String get(String s);
}
