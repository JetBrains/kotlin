// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 1 java/lang/invoke/LambdaMetafactory

// FILE: genericBoundInnerConstructorRef.kt
class Outer<TO>(val s1: TO) {
    inner class Inner<TI>(val s2: TI) {
        fun t() = s1.toString() + s2.toString()
    }
}

fun box() = Sam(Outer("O")::Inner).get("K").t()

// FILE: Sam.java
public interface Sam<TO, TI> {
    Outer<TO>.Inner<TI> get(String s);
}
