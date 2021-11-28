// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY
// FULL_JDK

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 2 java/lang/invoke/LambdaMetafactory
// 2 A\.plus\(Ljava/lang/String;\)Ljava/lang/String;\,
//  ^ check that we generate a reference to A::plus (same as javac)

// FILE: serializableFakeOverrideFunRef.kt
import java.io.*

abstract class A(val s: String) {
    fun plus(ss: String) = ss + s
}

abstract class B(s: String) : A(s)

class C(s: String) : B(s)

fun box(): String {
    return roundtrip(Sam(C::plus))
        .get(C("K"), "O")
}

fun <T> roundtrip(x: T): T {
    val out1 = ByteArrayOutputStream()
    ObjectOutputStream(out1).writeObject(x)
    return ObjectInputStream(ByteArrayInputStream(out1.toByteArray())).readObject() as T
}

// FILE: Sam.java
import java.io.*;

public interface Sam extends Serializable {
    String get(C x, String s);
}
