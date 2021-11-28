// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY
// FULL_JDK

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 3 java/lang/invoke/LambdaMetafactory

// FILE: serializableBoundInterfaceMemberFunRef.kt
import java.io.*

interface Plus {
    fun plus(ss: String): String
}

class C(val s: String) : Plus, Serializable {
    override fun plus(ss: String) = ss + s
}

class K : Plus, Serializable {
    override fun plus(ss: String) = ss + "K"
}

fun box(): String {
    val p1: Plus = C("K")
    val t1 = roundtrip(Sam(p1::plus)).get("O")
    if (t1 != "OK") return "Failed: t1='$t1'"

    val p2: Plus = K()
    val t2 = roundtrip(Sam(p2::plus)).get("O")
    if (t2 != "OK") return "Failed: t2='$t2'"

    return "OK"
}

fun <T> roundtrip(x: T): T {
    val out1 = ByteArrayOutputStream()
    ObjectOutputStream(out1).writeObject(x)
    return ObjectInputStream(ByteArrayInputStream(out1.toByteArray())).readObject() as T
}

// FILE: Sam.java
import java.io.*;

public interface Sam extends Serializable {
    String get(String s);
}
