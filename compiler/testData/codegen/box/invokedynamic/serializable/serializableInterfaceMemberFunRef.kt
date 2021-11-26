// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY
// FULL_JDK

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 2 java/lang/invoke/LambdaMetafactory

// FILE: serializableInterfaceMemberFunRef.kt
import java.io.*

interface Plus {
    fun plus(ss: String): String
}

class C(val s: String) : Plus {
    override fun plus(ss: String) = ss + s
}

fun box(): String {
    return roundtrip(Sam(Plus::plus))
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
    String get(Plus x, String s);
}
