// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY
// FULL_JDK

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 2 java/lang/invoke/LambdaMetafactory

// FILE: serializableBoundInnerConstructorRef.kt
import java.io.*

class Outer(val s1: String) : Serializable {
    inner class Inner (val s2: String) {
        fun test() = s1 + s2
    }
}

fun box(): String {
    return roundtrip(Sam(Outer("O")::Inner))
        .get("K").test()
}

fun <T> roundtrip(x: T): T {
    val out1 = ByteArrayOutputStream()
    ObjectOutputStream(out1).writeObject(x)
    return ObjectInputStream(ByteArrayInputStream(out1.toByteArray())).readObject() as T
}

// FILE: Sam.java
import java.io.*;

public interface Sam extends Serializable {
    Outer.Inner get(String s);
}
