// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 1 java/lang/invoke/LambdaMetafactory

// FILE: innerConstructorRef.kt
class Outer(val s1: String) {
    inner class Inner(val s2: String) {
        fun t() = s1 + s2
    }
}

fun box() = Sam(Outer::Inner).get(Outer("O"), "K").t()

// FILE: Sam.java
public interface Sam {
    Outer.Inner get(Outer outer, String s);
}
