// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 1 java/lang/invoke/LambdaMetafactory

// FILE: genericInnerConstructorRef.kt

// Can't resolve generic type argument for unbound constructor reference,
// so 'Outer' has no type parameters.
class Outer(val s1: String) {
    inner class Inner<TI>(val s2: TI) {
        fun t() = s1 + s2.toString()
    }
}

fun box() = Sam(Outer::Inner).get(Outer("O"), "K").t()

// FILE: Sam.java
public interface Sam<TI> {
    Outer.Inner<TI> get(Outer outer, String s);
}
