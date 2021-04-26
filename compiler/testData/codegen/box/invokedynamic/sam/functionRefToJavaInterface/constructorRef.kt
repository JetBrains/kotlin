// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 1 java/lang/invoke/LambdaMetafactory

// FILE: constructorRef.kt
class C(val t: String)

fun box() = Sam(::C).get("OK").t

// FILE: Sam.java
public interface Sam {
    C get(String s);
}
