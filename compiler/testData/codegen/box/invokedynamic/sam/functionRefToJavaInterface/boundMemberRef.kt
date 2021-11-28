// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 1 java/lang/invoke/LambdaMetafactory

// FILE: boundMemberRef.kt
class C(val t: String) {
    fun test() = t
}

fun box() = Sam(C("OK")::test).get()

// FILE: Sam.java
public interface Sam {
    String get();
}
