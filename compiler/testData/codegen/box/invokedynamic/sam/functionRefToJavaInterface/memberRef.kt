// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 1 java/lang/invoke/LambdaMetafactory

// FILE: memberRef.kt
class C(val t: String) {
    fun test() = t
}

fun box() = Sam(C::test).get(C("OK"))

// FILE: Sam.java
public interface Sam {
    String get(C c);
}
